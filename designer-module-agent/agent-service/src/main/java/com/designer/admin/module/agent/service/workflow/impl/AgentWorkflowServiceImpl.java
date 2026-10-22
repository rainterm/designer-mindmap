package com.designer.admin.module.agent.service.workflow.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.data.annotation.Transaction;
import org.noear.solon.web.sse.SseEmitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.common.constant.SystemConsts;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.common.util.JsonUtil;
import com.designer.admin.framework.satoken.LoginHelper;
import com.designer.admin.module.agent.constant.session.AgentSseEvent;
import com.designer.admin.module.agent.constant.workflow.WorkflowNodeType;
import com.designer.admin.module.agent.harness.AgentAsyncExecutor;
import com.designer.admin.module.agent.mapper.workflow.AgentWorkflowMapper;
import com.designer.admin.module.agent.mapper.workflow.AgentWorkflowVersionMapper;
import com.designer.admin.module.agent.model.dto.workflow.WorkflowQueryDTO;
import com.designer.admin.module.agent.model.dto.workflow.WorkflowSaveDTO;
import com.designer.admin.module.agent.model.entity.workflow.AgentWorkflow;
import com.designer.admin.module.agent.model.entity.workflow.AgentWorkflowVersion;
import com.designer.admin.module.agent.model.vo.workflow.WorkflowRunResult;
import com.designer.admin.module.agent.model.vo.workflow.WorkflowStartInputVO;
import com.designer.admin.module.agent.model.vo.workflow.WorkflowVO;
import com.designer.admin.module.agent.model.vo.workflow.WorkflowVersionVO;
import com.designer.admin.module.agent.service.workflow.AgentWorkflowService;
import com.designer.admin.module.agent.workflow.AgentWorkflowExecutor;
import com.designer.admin.module.agent.workflow.AgentWorkflowStartInputs;
import com.designer.admin.module.agent.workflow.WorkflowRunListener;
import com.designer.admin.module.agent.util.AgentEntitySupport;
import com.designer.admin.module.agent.util.AgentJson;
import com.designer.admin.module.agent.util.AgentSseSupport;
import com.designer.admin.module.agent.util.AgentTexts;

/**
 * 工作流管理
 *
 * @author 雨夜终端
 */
@Component
public class AgentWorkflowServiceImpl implements AgentWorkflowService {

    private static final Logger log = LoggerFactory.getLogger(AgentWorkflowServiceImpl.class);

    @Db
    private AgentWorkflowMapper workflowMapper;
    @Db
    private AgentWorkflowVersionMapper versionMapper;
    @Inject
    private AgentWorkflowExecutor workflowExecutor;
    @Inject
    private AgentAsyncExecutor asyncExecutor;

    @Override
    public IPage<WorkflowVO> page(WorkflowQueryDTO query) {
        IPage<AgentWorkflow> page = workflowMapper.selectPage(query.toPage(), new LambdaQueryWrapper<AgentWorkflow>()
                .and(AgentTexts.notBlank(query.getName()), w -> w
                        .like(AgentWorkflow::getName, query.getName())
                        .or()
                        .like(AgentWorkflow::getCode, query.getName()))
                .eq(query.getStatus() != null, AgentWorkflow::getStatus, query.getStatus())
                .orderByDesc(AgentWorkflow::getId));
        return page.convert(this::toVo);
    }

    @Override
    public List<WorkflowVO> listEnabled() {
        return workflowMapper.selectList(new LambdaQueryWrapper<AgentWorkflow>()
                        .eq(AgentWorkflow::getStatus, SystemConsts.STATUS_ENABLE)
                        .orderByDesc(AgentWorkflow::getId))
                .stream()
                .map(this::toVo)
                .toList();
    }

    @Override
    public List<WorkflowVO> listPublished() {
        return workflowMapper.selectList(new LambdaQueryWrapper<AgentWorkflow>()
                        .eq(AgentWorkflow::getStatus, SystemConsts.STATUS_ENABLE)
                        .gt(AgentWorkflow::getVersion, 0)
                        .orderByDesc(AgentWorkflow::getId))
                .stream()
                .map(this::toPublishedSummaryVo)
                .toList();
    }

    @Override
    public WorkflowVO getById(Long id) {
        return toVo(require(id));
    }

    @Override
    public WorkflowVO getPublishedById(Long id) {
        return toPublishedVo(requirePublished(id));
    }

    @Override
    public WorkflowVO save(WorkflowSaveDTO dto) {
        validate(dto, null);
        AgentWorkflow entity = toEntity(dto);
        entity.setVersion(1);
        entity.setDraftVersion(1);
        if (entity.getStatus() == null) {
            entity.setStatus(SystemConsts.STATUS_ENABLE);
        }
        // 新建即视为已发布的 v1：草稿与正式内容一致，创建后立即可运行
        entity.setDraftNodesJson(entity.getNodesJson());
        entity.setDraftEdgesJson(entity.getEdgesJson());
        workflowMapper.insert(entity);
        return toVo(entity);
    }

    @Override
    public WorkflowVO update(WorkflowSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("工作流ID不能为空");
        }
        AgentWorkflow existing = require(dto.getId());
        validate(dto, dto.getId());
        AgentWorkflow entity = toEntity(dto);
        entity.setId(existing.getId());
        entity.setVersion(existing.getVersion());
        entity.setDraftVersion(AgentEntitySupport.bumpDraftVersion(existing.getDraftVersion()));
        // 草稿列：有图则更新；无图（列表页基本信息编辑）沿用现有草稿
        if (dto.getNodes() == null) {
            entity.setDraftNodesJson(draftNodesOf(existing));
        }
        if (dto.getEdges() == null) {
            entity.setDraftEdgesJson(draftEdgesOf(existing));
        }
        // 正式列保持 null：MyBatis-Plus updateById 忽略 null 字段，已发布内容不受影响
        entity.setNodesJson(null);
        entity.setEdgesJson(null);
        workflowMapper.updateById(entity);
        return toVo(workflowMapper.selectById(entity.getId()));
    }

    @Override
    @Transaction
    public WorkflowVO publish(Long id) {
        AgentWorkflow existing = require(id);
        int nextVersion = AgentEntitySupport.nextPublishVersion(existing.getVersion());
        // 待发布内容：优先草稿；草稿为空（旧数据未回填）时退回正式内容
        String nodesJson = draftNodesOf(existing);
        String edgesJson = draftEdgesOf(existing);
        // 已存在同版本快照：覆盖（幂等发布）
        AgentWorkflowVersion snapshot = versionMapper.selectOne(new LambdaQueryWrapper<AgentWorkflowVersion>()
                .eq(AgentWorkflowVersion::getWorkflowId, id)
                .eq(AgentWorkflowVersion::getVersion, nextVersion));
        if (snapshot == null) {
            snapshot = new AgentWorkflowVersion();
        }
        snapshot.setWorkflowId(id);
        snapshot.setVersion(nextVersion);
        snapshot.setName(existing.getName());
        snapshot.setCode(existing.getCode());
        snapshot.setDescription(existing.getDescription());
        snapshot.setNodesJson(nodesJson);
        snapshot.setEdgesJson(edgesJson);
        snapshot.setStatus(existing.getStatus());
        snapshot.setRemark(existing.getRemark());
        snapshot.setPublishedBy(LoginHelper.getUsername());
        snapshot.setPublishedAt(LocalDateTime.now());
        if (snapshot.getId() == null) {
            versionMapper.insert(snapshot);
        } else {
            versionMapper.updateById(snapshot);
        }

        // 草稿 → 正式，运行即刻切换到新版本内容
        existing.setNodesJson(nodesJson);
        existing.setEdgesJson(edgesJson);
        existing.setVersion(nextVersion);
        // 草稿版本号与已发布对齐（当前草稿内容即为该版本）
        existing.setDraftVersion(nextVersion);
        workflowMapper.updateById(existing);
        return toVo(workflowMapper.selectById(id));
    }

    @Override
    public IPage<WorkflowVersionVO> listHistory(Long id, long pageNum, long pageSize) {
        require(id);
        IPage<AgentWorkflowVersion> p = versionMapper.selectPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<AgentWorkflowVersion>()
                        .eq(AgentWorkflowVersion::getWorkflowId, id)
                        .orderByDesc(AgentWorkflowVersion::getVersion));
        return p.convert(this::toVersionVo);
    }

    @Override
    public WorkflowVersionVO getVersion(Long id, Integer version) {
        if (version == null) {
            throw new BizException("版本号不能为空");
        }
        AgentWorkflowVersion row = versionMapper.selectOne(new LambdaQueryWrapper<AgentWorkflowVersion>()
                .eq(AgentWorkflowVersion::getWorkflowId, id)
                .eq(AgentWorkflowVersion::getVersion, version));
        if (row == null) {
            throw new BizException("历史版本不存在");
        }
        WorkflowVersionVO vo = toVersionVo(row);
        vo.setNodes(AgentJson.parseMapList(row.getNodesJson()));
        vo.setEdges(AgentJson.parseMapList(row.getEdgesJson()));
        return vo;
    }

    @Override
    @Transaction
    public WorkflowVO rollback(Long id, Integer targetVersion) {
        AgentWorkflow existing = require(id);
        AgentWorkflowVersion snap = versionMapper.selectOne(new LambdaQueryWrapper<AgentWorkflowVersion>()
                .eq(AgentWorkflowVersion::getWorkflowId, id)
                .eq(AgentWorkflowVersion::getVersion, targetVersion));
        if (snap == null) {
            throw new BizException("历史版本不存在");
        }
        // 只覆盖草稿图；其余字段不动
        existing.setDraftNodesJson(snap.getNodesJson());
        existing.setDraftEdgesJson(snap.getEdgesJson());
        // draftVersion 递增以标识草稿发生了一次内容变更
        existing.setDraftVersion(AgentEntitySupport.bumpDraftVersion(existing.getDraftVersion()));
        workflowMapper.updateById(existing);
        return toVo(workflowMapper.selectById(id));
    }

    @Override
    public void remove(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        AgentEntitySupport.deleteBatchIds(workflowMapper, ids);
    }

    @Override
    public SseEmitter runStream(Long id, Map<String, Object> params, Long agentId) {
        return startRunStream(id, params, agentId, true);
    }

    @Override
    public SseEmitter runPublishedStream(Long id, Map<String, Object> params, Long agentId) {
        return startRunStream(id, params, agentId, false);
    }

    private SseEmitter startRunStream(Long id, Map<String, Object> params, Long agentId, boolean useDraft) {
        require(id);
        AtomicBoolean closed = new AtomicBoolean(false);
        final SseEmitter[] box = new SseEmitter[1];
        SseEmitter emitter = new SseEmitter(-1L)
                .onCompletion(() -> closed.set(true))
                .onError(e -> closed.set(true))
                .onInited(s -> {
                    box[0] = s;
                    asyncExecutor.execute(() -> doRunStream(s, id, params, agentId, useDraft, closed));
                });
        box[0] = emitter;
        return emitter;
    }

    private void doRunStream(SseEmitter emitter, Long id, Map<String, Object> params, Long agentId,
                             boolean useDraft, AtomicBoolean closed) {
        try {
            WorkflowRunListener listener = new WorkflowRunListener() {
                @Override
                public void onStatus(String status) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("status", status == null ? "" : status);
                    AgentSseSupport.safeSend(emitter, closed, AgentSseEvent.STATUS.getCode(), JsonUtil.toJson(m));
                }

                @Override
                public void onStep(WorkflowRunResult.Step step) {
                    AgentSseSupport.safeSend(emitter, closed, AgentSseEvent.STEP.getCode(), JsonUtil.toJson(step));
                }

                @Override
                public void onAnswerDelta(String delta) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("delta", delta == null ? "" : delta);
                    AgentSseSupport.safeSend(emitter, closed, AgentSseEvent.DELTA.getCode(), JsonUtil.toJson(m));
                }

                @Override
                public void onThinkingDelta(String delta) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("delta", delta == null ? "" : delta);
                    AgentSseSupport.safeSend(emitter, closed, AgentSseEvent.THINKING.getCode(), JsonUtil.toJson(m));
                }

                @Override
                public void onError(String message) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("msg", message == null ? "执行失败" : message);
                    AgentSseSupport.safeSend(emitter, closed, AgentSseEvent.ERROR.getCode(), JsonUtil.toJson(m));
                }
            };
            WorkflowRunResult result = workflowExecutor.run(id, params, agentId, listener, useDraft);
            AgentSseSupport.safeSend(emitter, closed, AgentSseEvent.DONE.getCode(), JsonUtil.toJson(result));
        } catch (Exception e) {
            log.warn("工作流 SSE 试跑失败 id={}: {}", id, e.getMessage());
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("msg", e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            AgentSseSupport.safeSend(emitter, closed, AgentSseEvent.ERROR.getCode(), JsonUtil.toJson(err));
        } finally {
            if (!closed.get()) {
                try {
                    emitter.complete();
                } catch (Exception ignored) {
                    // ignore
                }
            }
        }
    }

    private void validate(WorkflowSaveDTO dto, Long excludeId) {
        if (!AgentTexts.notBlank(dto.getName())) {
            throw new BizException("工作流名称不能为空");
        }
        if (!AgentTexts.notBlank(dto.getCode())) {
            throw new BizException("工作流编码不能为空");
        }
        Long cnt = workflowMapper.selectCount(new LambdaQueryWrapper<AgentWorkflow>()
                .eq(AgentWorkflow::getCode, dto.getCode().trim())
                .ne(excludeId != null, AgentWorkflow::getId, excludeId));
        AgentEntitySupport.assertCodeUnique(cnt, "工作流编码已存在");
    }

    private AgentWorkflow require(Long id) {
        return AgentEntitySupport.require(workflowMapper.selectById(id), "工作流不存在");
    }

    private AgentWorkflow requirePublished(Long id) {
        AgentWorkflow wf = require(id);
        if (!Objects.equals(wf.getStatus(), SystemConsts.STATUS_ENABLE)) {
            throw new BizException("工作流已停用");
        }
        if (wf.getVersion() == null || wf.getVersion() <= 0) {
            throw new BizException("工作流尚未发布");
        }
        return wf;
    }

    private AgentWorkflow toEntity(WorkflowSaveDTO dto) {
        AgentWorkflow e = new AgentWorkflow();
        e.setId(dto.getId());
        e.setName(dto.getName().trim());
        e.setCode(dto.getCode().trim());
        e.setDescription(dto.getDescription() == null ? "" : dto.getDescription());
        e.setNodesJson(AgentJson.toJson(normalizeNodes(dto.getNodes())));
        e.setEdgesJson(AgentJson.toJson(normalizeEdges(dto.getEdges())));
        e.setStatus(dto.getStatus());
        e.setRemark(dto.getRemark() == null ? "" : dto.getRemark());
        return e;
    }

    private WorkflowVO toVo(AgentWorkflow e) {
        // 编辑/试跑面向草稿：优先草稿列；为空（旧数据）退回正式列
        List<Map<String, Object>> nodes = AgentJson.parseMapList(draftNodesOf(e));
        List<Map<String, Object>> edges = AgentJson.parseMapList(draftEdgesOf(e));
        return fillWorkflowVo(e, nodes, edges);
    }

    /** 开放接口列表：不含图，避免泄漏草稿与大图；仍返回开始节点入参 */
    private WorkflowVO toPublishedSummaryVo(AgentWorkflow e) {
        List<Map<String, Object>> nodes = AgentJson.parseMapList(e.getNodesJson());
        WorkflowVO vo = fillWorkflowVo(e, List.of(), List.of());
        vo.setNodeCount(nodes.size());
        vo.setStartInputs(toStartInputVos(AgentWorkflowStartInputs.parseFromNodes(nodes)));
        return vo;
    }

    /** 开放接口详情：正式 nodes/edges */
    private WorkflowVO toPublishedVo(AgentWorkflow e) {
        List<Map<String, Object>> nodes = AgentJson.parseMapList(e.getNodesJson());
        List<Map<String, Object>> edges = AgentJson.parseMapList(e.getEdgesJson());
        return fillWorkflowVo(e, nodes, edges);
    }

    private static WorkflowVO fillWorkflowVo(AgentWorkflow e, List<Map<String, Object>> nodes,
                                             List<Map<String, Object>> edges) {
        WorkflowVO vo = new WorkflowVO();
        vo.setId(e.getId());
        vo.setName(e.getName());
        vo.setCode(e.getCode());
        vo.setDescription(e.getDescription());
        vo.setNodes(nodes);
        vo.setEdges(edges);
        vo.setStartInputs(toStartInputVos(AgentWorkflowStartInputs.parseFromNodes(nodes)));
        vo.setNodeCount(nodes == null ? 0 : nodes.size());
        vo.setVersion(e.getVersion());
        vo.setDraftVersion(e.getDraftVersion());
        vo.setStatus(e.getStatus());
        vo.setRemark(e.getRemark());
        vo.setCreateTime(e.getCreateTime());
        vo.setUpdateTime(e.getUpdateTime());
        return vo;
    }

    private static List<WorkflowStartInputVO> toStartInputVos(List<AgentWorkflowStartInputs.Spec> specs) {
        if (specs == null || specs.isEmpty()) {
            return List.of();
        }
        return specs.stream().map(s -> {
            WorkflowStartInputVO vo = new WorkflowStartInputVO();
            vo.setKey(s.key());
            vo.setLabel(s.label());
            vo.setRequired(s.required());
            vo.setDefaultValue(s.defaultValue() == null ? "" : s.defaultValue());
            return vo;
        }).toList();
    }

    /** 草稿节点图；为空（未回填的旧数据）时退回已发布图 */
    private static String draftNodesOf(AgentWorkflow e) {
        return AgentTexts.notBlank(e.getDraftNodesJson()) ? e.getDraftNodesJson() : e.getNodesJson();
    }

    /** 草稿连线图；为空（未回填的旧数据）时退回已发布图 */
    private static String draftEdgesOf(AgentWorkflow e) {
        return AgentTexts.notBlank(e.getDraftEdgesJson()) ? e.getDraftEdgesJson() : e.getEdgesJson();
    }

    private WorkflowVersionVO toVersionVo(AgentWorkflowVersion e) {
        WorkflowVersionVO vo = new WorkflowVersionVO();
        vo.setId(e.getId());
        vo.setWorkflowId(e.getWorkflowId());
        vo.setVersion(e.getVersion());
        vo.setName(e.getName());
        vo.setCode(e.getCode());
        vo.setDescription(e.getDescription());
        vo.setStatus(e.getStatus());
        vo.setRemark(e.getRemark());
        vo.setPublishedBy(e.getPublishedBy());
        vo.setPublishedAt(e.getPublishedAt());
        return vo;
    }

    private static List<Map<String, Object>> normalizeNodes(List<Map<String, Object>> nodes) {
        if (nodes == null) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>(nodes.size());
        for (int i = 0; i < nodes.size(); i++) {
            Map<String, Object> n = nodes.get(i) == null ? Map.of() : nodes.get(i);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("key", strOr(n.get("key"), "n" + (i + 1)));
            String type = strOr(n.get("type"), "llm");
            row.put("type", type);
            row.put("name", strOr(n.get("name"), "节点" + (i + 1)));
            row.put("config", AgentJson.configAsString(n.get("config")));
            row.put("outputVar", WorkflowNodeType.OUTPUT.eqIgnoreCase(type) ? "" : strOr(n.get("outputVar"), ""));
            row.put("x", numberOr(n.get("x"), 80 + (i % 4) * 220));
            row.put("y", numberOr(n.get("y"), 120 + (i / 4) * 140));
            result.add(row);
        }
        return result;
    }

    private static List<Map<String, Object>> normalizeEdges(List<Map<String, Object>> edges) {
        if (edges == null) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        int i = 0;
        for (Map<String, Object> e : edges) {
            if (e == null || e.get("source") == null || e.get("target") == null) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", strOr(e.get("id"), "e" + (++i)));
            row.put("source", String.valueOf(e.get("source")));
            row.put("target", String.valueOf(e.get("target")));
            row.put("sourceHandle", e.get("sourceHandle"));
            row.put("targetHandle", e.get("targetHandle"));
            row.put("label", e.get("label") == null ? "" : e.get("label"));
            result.add(row);
        }
        return result;
    }

    private static String strOr(Object v, String def) {
        if (v == null || String.valueOf(v).isBlank()) {
            return def;
        }
        return String.valueOf(v);
    }

    private static Number numberOr(Object v, int def) {
        if (v instanceof Number n) {
            return n;
        }
        if (v != null) {
            try {
                return Double.parseDouble(String.valueOf(v));
            } catch (NumberFormatException ignored) {
                // fallthrough
            }
        }
        return def;
    }
}
