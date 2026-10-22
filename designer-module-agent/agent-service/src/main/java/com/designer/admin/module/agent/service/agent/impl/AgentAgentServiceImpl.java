package com.designer.admin.module.agent.service.agent.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.data.annotation.Transaction;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.designer.admin.common.constant.SystemConsts;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.framework.satoken.LoginHelper;
import com.designer.admin.module.agent.mapper.agent.AgentAgentMapper;
import com.designer.admin.module.agent.mapper.agent.AgentAgentVersionMapper;
import com.designer.admin.module.agent.mapper.provider.AgentProviderMapper;
import com.designer.admin.module.agent.model.dto.agent.AgentQueryDTO;
import com.designer.admin.module.agent.model.dto.agent.AgentSaveDTO;
import com.designer.admin.module.agent.model.entity.agent.AgentAgent;
import com.designer.admin.module.agent.model.entity.agent.AgentAgentVersion;
import com.designer.admin.module.agent.model.entity.provider.AgentProvider;
import com.designer.admin.module.agent.model.vo.agent.AgentVO;
import com.designer.admin.module.agent.model.vo.agent.AgentVersionVO;
import com.designer.admin.module.agent.service.agent.AgentAgentService;
import com.designer.admin.module.agent.constant.AgentConsts;
import com.designer.admin.module.agent.util.AgentEntitySupport;
import com.designer.admin.module.agent.util.AgentJson;
import com.designer.admin.module.agent.util.AgentTexts;

/**
 * 智能体管理
 *
 * @author 雨夜终端
 */
@Component
public class AgentAgentServiceImpl implements AgentAgentService {

    @Db
    private AgentAgentMapper agentMapper;
    @Db
    private AgentAgentVersionMapper versionMapper;
    @Db
    private AgentProviderMapper providerMapper;

    @Override
    public IPage<AgentVO> page(AgentQueryDTO query) {
        IPage<AgentAgent> page = agentMapper.selectPage(query.toPage(), new LambdaQueryWrapper<AgentAgent>()
                .and(AgentTexts.notBlank(query.getName()), w -> w
                        .like(AgentAgent::getName, query.getName())
                        .or()
                        .like(AgentAgent::getCode, query.getName()))
                .eq(query.getStatus() != null, AgentAgent::getStatus, query.getStatus())
                .orderByDesc(AgentAgent::getId));
        Map<Long, String> providerNames = loadProviderNames(page.getRecords());
        return page.convert(a -> toVo(a, providerNames.get(a.getProviderId())));
    }

    @Override
    public IPage<AgentVO> pageStore(AgentQueryDTO query) {
        IPage<AgentAgent> page = agentMapper.selectPage(query.toPage(), new LambdaQueryWrapper<AgentAgent>()
                .eq(AgentAgent::getStatus, SystemConsts.STATUS_ENABLE)
                // version>0 表示已发布；从未发布（草稿态）不进入商店
                .gt(AgentAgent::getVersion, 0)
                .and(AgentTexts.notBlank(query.getName()), w -> w
                        .like(AgentAgent::getName, query.getName())
                        .or()
                        .like(AgentAgent::getCode, query.getName()))
                .orderByDesc(AgentAgent::getId));
        Map<Long, String> providerNames = loadProviderNames(page.getRecords());
        return page.convert(a -> toVo(a, providerNames.get(a.getProviderId())));
    }

    @Override
    public List<AgentVO> listEnabled() {
        List<AgentAgent> list = agentMapper.selectList(new LambdaQueryWrapper<AgentAgent>()
                .eq(AgentAgent::getStatus, SystemConsts.STATUS_ENABLE)
                .orderByDesc(AgentAgent::getId));
        Map<Long, String> providerNames = loadProviderNames(list);
        return list.stream()
                .map(a -> toVo(a, providerNames.get(a.getProviderId())))
                .toList();
    }

    @Override
    public List<AgentVO> listPublished() {
        List<AgentAgent> list = agentMapper.selectList(new LambdaQueryWrapper<AgentAgent>()
                .eq(AgentAgent::getStatus, SystemConsts.STATUS_ENABLE)
                .gt(AgentAgent::getVersion, 0)
                .orderByDesc(AgentAgent::getId));
        Map<Long, String> providerNames = loadProviderNames(list);
        return list.stream()
                .map(a -> toVo(a, providerNames.get(a.getProviderId())))
                .toList();
    }

    @Override
    public AgentVO getById(Long id) {
        AgentAgent agent = require(id);
        String draftJson = draftJsonOf(agent);
        AgentSaveDTO draft = AgentJson.parse(draftJson, AgentSaveDTO.class);
        Long providerId = draft != null && draft.getProviderId() != null ? draft.getProviderId() : agent.getProviderId();
        String providerName = null;
        if (providerId != null) {
            AgentProvider p = providerMapper.selectById(providerId);
            providerName = p == null ? null : p.getName();
        }
        AgentVO vo = toVo(agent, providerName);
        if (draft != null) {
            applyDraftToVo(vo, draft);
        }
        return vo;
    }

    @Override
    public AgentVO getPublishedById(Long id) {
        AgentAgent agent = requirePublished(id);
        String providerName = null;
        if (agent.getProviderId() != null) {
            AgentProvider p = providerMapper.selectById(agent.getProviderId());
            providerName = p == null ? null : p.getName();
        }
        return toVo(agent, providerName);
    }

    @Override
    public AgentVO save(AgentSaveDTO dto) {
        validate(dto, null);
        if (!AgentTexts.notBlank(dto.getCode())) {
            dto.setCode(nextAgentCode());
        }
        AgentAgent entity = toEntity(dto);
        if (entity.getStatus() == null) {
            entity.setStatus(SystemConsts.STATUS_ENABLE);
        }
        // 新建即草稿态：version=0 表示从未发布，商店不展示；首次发布后才为 v1
        entity.setVersion(0);
        entity.setDraftVersion(1);
        entity.setDraftJson(AgentJson.toJson(dto));
        agentMapper.insert(entity);
        return getById(entity.getId());
    }

    @Override
    public AgentVO update(AgentSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("智能体ID不能为空");
        }
        AgentAgent existing = require(dto.getId());
        if (!AgentTexts.notBlank(dto.getCode())) {
            dto.setCode(existing.getCode());
        }
        validate(dto, dto.getId());
        int nextDraft = AgentEntitySupport.bumpDraftVersion(existing.getDraftVersion());
        agentMapper.update(null, new LambdaUpdateWrapper<AgentAgent>()
                .eq(AgentAgent::getId, dto.getId())
                .set(AgentAgent::getName, dto.getName().trim())
                .set(AgentAgent::getCode, dto.getCode().trim())
                .set(AgentAgent::getAvatar, dto.getAvatar() == null ? "" : dto.getAvatar())
                .set(AgentAgent::getStatus, dto.getStatus() == null ? existing.getStatus() : dto.getStatus())
                .set(AgentAgent::getRemark, dto.getRemark() == null ? "" : dto.getRemark())
                .set(AgentAgent::getDraftJson, AgentJson.toJson(dto))
                .set(AgentAgent::getDraftVersion, nextDraft));
        return getById(dto.getId());
    }

    @Override
    @Transaction
    public AgentVO publish(Long id) {
        AgentAgent existing = require(id);
        String draftJson = draftJsonOf(existing);
        AgentSaveDTO draft = AgentJson.parse(draftJson, AgentSaveDTO.class);
        if (draft == null) {
            throw new BizException("草稿内容缺失，无法发布");
        }
        int nextVersion = AgentEntitySupport.nextPublishVersion(existing.getVersion());
        // 幂等发布：已存在同版本快照则覆盖
        AgentAgentVersion snapshot = versionMapper.selectOne(new LambdaQueryWrapper<AgentAgentVersion>()
                .eq(AgentAgentVersion::getAgentId, id)
                .eq(AgentAgentVersion::getVersion, nextVersion));
        if (snapshot == null) {
            snapshot = new AgentAgentVersion();
        }
        snapshot.setAgentId(id);
        snapshot.setVersion(nextVersion);
        snapshot.setConfigJson(draftJson);
        snapshot.setName(existing.getName());
        snapshot.setCode(existing.getCode());
        snapshot.setStatus(existing.getStatus());
        snapshot.setRemark(existing.getRemark());
        snapshot.setPublishedBy(LoginHelper.getUsername());
        snapshot.setPublishedAt(LocalDateTime.now());
        if (snapshot.getId() == null) {
            versionMapper.insert(snapshot);
        } else {
            versionMapper.updateById(snapshot);
        }

        // 草稿 → 正式，运行即刻切换到新版本配置
        applyConfig(existing, draft);
        existing.setVersion(nextVersion);
        existing.setDraftVersion(nextVersion);
        agentMapper.updateById(existing);
        return getById(id);
    }

    @Override
    public IPage<AgentVersionVO> listHistory(Long id, long pageNum, long pageSize) {
        require(id);
        IPage<AgentAgentVersion> p = versionMapper.selectPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<AgentAgentVersion>()
                        .eq(AgentAgentVersion::getAgentId, id)
                        .orderByDesc(AgentAgentVersion::getVersion));
        return p.convert(this::toVersionVo);
    }

    @Override
    public AgentVersionVO getVersion(Long id, Integer version) {
        if (version == null) {
            throw new BizException("版本号不能为空");
        }
        AgentAgentVersion row = versionMapper.selectOne(new LambdaQueryWrapper<AgentAgentVersion>()
                .eq(AgentAgentVersion::getAgentId, id)
                .eq(AgentAgentVersion::getVersion, version));
        if (row == null) {
            throw new BizException("历史版本不存在");
        }
        AgentVersionVO vo = toVersionVo(row);
        vo.setConfig(AgentJson.parseMap(row.getConfigJson()));
        return vo;
    }

    @Override
    @Transaction
    public AgentVO rollback(Long id, Integer targetVersion) {
        AgentAgent existing = require(id);
        AgentAgentVersion snap = versionMapper.selectOne(new LambdaQueryWrapper<AgentAgentVersion>()
                .eq(AgentAgentVersion::getAgentId, id)
                .eq(AgentAgentVersion::getVersion, targetVersion));
        if (snap == null) {
            throw new BizException("历史版本不存在");
        }
        existing.setDraftJson(snap.getConfigJson());
        // draftVersion 递增以标识草稿发生了一次内容变更
        existing.setDraftVersion(AgentEntitySupport.bumpDraftVersion(existing.getDraftVersion()));
        agentMapper.updateById(existing);
        return getById(id);
    }

    @Override
    public void remove(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        AgentEntitySupport.deleteBatchIds(agentMapper, ids);
    }

    @Override
    public AgentAgent requireEntity(Long id) {
        return require(id);
    }

    @Override
    public AgentVO enablePublic(Long id) {
        AgentAgent agent = requirePublished(id);
        String token = AgentTexts.notBlank(agent.getPublicToken())
                ? agent.getPublicToken()
                : newPublicToken();
        agentMapper.update(null, new LambdaUpdateWrapper<AgentAgent>()
                .eq(AgentAgent::getId, id)
                .set(AgentAgent::getPublicEnabled, 1)
                .set(AgentAgent::getPublicToken, token));
        return getById(id);
    }

    @Override
    public AgentVO disablePublic(Long id) {
        require(id);
        agentMapper.update(null, new LambdaUpdateWrapper<AgentAgent>()
                .eq(AgentAgent::getId, id)
                .set(AgentAgent::getPublicEnabled, 0)
                .set(AgentAgent::getPublicToken, ""));
        return getById(id);
    }

    @Override
    public AgentVO rotatePublic(Long id) {
        AgentAgent agent = requirePublished(id);
        if (agent.getPublicEnabled() == null || agent.getPublicEnabled() != 1) {
            throw new BizException("请先开启公开访问");
        }
        agentMapper.update(null, new LambdaUpdateWrapper<AgentAgent>()
                .eq(AgentAgent::getId, id)
                .set(AgentAgent::getPublicToken, newPublicToken()));
        return getById(id);
    }

    @Override
    public AgentAgent requireByPublicToken(String publicToken) {
        if (!AgentTexts.notBlank(publicToken)) {
            throw new BizException("公开链接无效");
        }
        AgentAgent agent = agentMapper.selectOne(new LambdaQueryWrapper<AgentAgent>()
                .eq(AgentAgent::getPublicToken, publicToken.trim())
                .eq(AgentAgent::getPublicEnabled, 1)
                .last("LIMIT 1"));
        if (agent == null) {
            throw new BizException("公开链接无效或已关闭");
        }
        if (!Objects.equals(agent.getStatus(), SystemConsts.STATUS_ENABLE)
                || agent.getVersion() == null || agent.getVersion() <= 0) {
            throw new BizException("智能体不可用");
        }
        return agent;
    }

    @Override
    public void applyDraftConfig(AgentAgent agent) {
        if (agent == null) {
            return;
        }
        AgentSaveDTO draft = AgentJson.parse(draftJsonOf(agent), AgentSaveDTO.class);
        if (draft != null) {
            applyConfig(agent, draft);
        }
    }

    private void validate(AgentSaveDTO dto, Long excludeId) {
        if (!AgentTexts.notBlank(dto.getName())) {
            throw new BizException("智能体名称不能为空");
        }
        if (AgentTexts.notBlank(dto.getCode())) {
            Long cnt = agentMapper.selectCount(new LambdaQueryWrapper<AgentAgent>()
                    .eq(AgentAgent::getCode, dto.getCode().trim())
                    .ne(excludeId != null, AgentAgent::getId, excludeId));
            AgentEntitySupport.assertCodeUnique(cnt, "智能体编码已存在");
        }
    }

    /** 库表仍保留唯一 code，前端不展示时由服务端生成。 */
    private String nextAgentCode() {
        for (int i = 0; i < 5; i++) {
            String code = AgentConsts.CODE_PREFIX_AGENT + Long.toString(System.currentTimeMillis(), 36)
                    + Integer.toString((int) (Math.random() * 36 * 36), 36);
            Long cnt = agentMapper.selectCount(new LambdaQueryWrapper<AgentAgent>()
                    .eq(AgentAgent::getCode, code));
            if (cnt == null || cnt == 0) {
                return code;
            }
        }
        return AgentConsts.CODE_PREFIX_AGENT + java.util.UUID.randomUUID().toString().replace("-", "");
    }

    private AgentAgent require(Long id) {
        return AgentEntitySupport.require(agentMapper.selectById(id), "智能体不存在");
    }

    /** 已启用且已发布；开放接口 / 公开链接共用 */
    private AgentAgent requirePublished(Long id) {
        AgentAgent agent = require(id);
        if (!Objects.equals(agent.getStatus(), SystemConsts.STATUS_ENABLE)) {
            throw new BizException("智能体已停用");
        }
        if (agent.getVersion() == null || agent.getVersion() <= 0) {
            throw new BizException("智能体尚未发布");
        }
        return agent;
    }

    private static String newPublicToken() {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }

    private AgentAgent toEntity(AgentSaveDTO dto) {
        AgentAgent e = new AgentAgent();
        e.setId(dto.getId());
        e.setName(dto.getName().trim());
        e.setCode(dto.getCode().trim());
        e.setAvatar(dto.getAvatar() == null ? "" : dto.getAvatar());
        e.setSystemPrompt(dto.getSystemPrompt() == null ? "" : dto.getSystemPrompt());
        e.setProviderId(dto.getProviderId());
        e.setModelCode(dto.getModelCode() == null ? "" : dto.getModelCode());
        e.setTemperature(dto.getTemperature() == null ? AgentConsts.DEFAULT_TEMPERATURE : dto.getTemperature());
        e.setTopP(dto.getTopP() == null ? AgentConsts.DEFAULT_TOP_P : dto.getTopP());
        // 0 表示不限制（不向模型传 max_tokens）
        e.setMaxTokens(dto.getMaxTokens() != null && dto.getMaxTokens() > 0 ? dto.getMaxTokens() : 0);
        e.setMemoryRounds(normalizeMemoryRounds(dto.getMemoryRounds()));
        e.setToolIdsJson(AgentJson.toJson(dto.getToolIds() == null ? List.of() : dto.getToolIds()));
        e.setMcpIdsJson(AgentJson.toJson(dto.getMcpIds() == null ? List.of() : dto.getMcpIds()));
        e.setSkillIdsJson(AgentJson.toJson(dto.getSkillIds() == null ? List.of() : dto.getSkillIds()));
        e.setWorkflowIdsJson(AgentJson.toJson(dto.getWorkflowIds() == null ? List.of() : dto.getWorkflowIds()));
        e.setKnowledgeIdsJson(AgentJson.toJson(dto.getKnowledgeIds() == null ? List.of() : dto.getKnowledgeIds()));
        e.setEmbeddingProviderId(dto.getEmbeddingProviderId());
        e.setEmbeddingModel(dto.getEmbeddingModel() == null ? "" : dto.getEmbeddingModel().trim());
        e.setRerankProviderId(dto.getRerankProviderId());
        e.setRerankModel(dto.getRerankModel() == null ? "" : dto.getRerankModel().trim());
        e.setRagTopK(dto.getRagTopK() == null || dto.getRagTopK() < 1 ? AgentConsts.DEFAULT_TOP_K
                : Math.min(AgentConsts.MAX_TOP_K, dto.getRagTopK()));
        e.setRagRecallTopK(normalizeRecallTopK(dto.getRagRecallTopK()));
        e.setRagScoreThreshold(dto.getRagScoreThreshold() == null
                ? AgentConsts.DEFAULT_SCORE_THRESHOLD : dto.getRagScoreThreshold());
        e.setFileEnabled(dto.getFileEnabled() == null ? 0 : (dto.getFileEnabled() == 1 ? 1 : 0));
        e.setStatus(dto.getStatus());
        e.setRemark(dto.getRemark() == null ? "" : dto.getRemark());
        return e;
    }

    private static int normalizeRecallTopK(Integer value) {
        if (value == null || value < 0) {
            return 0;
        }
        return Math.min(AgentConsts.MAX_TOP_K, value);
    }

    private AgentVO toVo(AgentAgent e, String providerName) {
        AgentVO vo = new AgentVO();
        vo.setId(e.getId());
        vo.setName(e.getName());
        vo.setCode(e.getCode());
        vo.setAvatar(e.getAvatar());
        vo.setSystemPrompt(e.getSystemPrompt());
        vo.setProviderId(e.getProviderId());
        vo.setProviderName(providerName == null ? "" : providerName);
        vo.setModelCode(e.getModelCode());
        vo.setTemperature(e.getTemperature());
        vo.setTopP(e.getTopP());
        vo.setMaxTokens(e.getMaxTokens());
        vo.setMemoryRounds(normalizeMemoryRounds(e.getMemoryRounds()));
        vo.setToolIds(AgentJson.parseLongList(e.getToolIdsJson()));
        vo.setMcpIds(AgentJson.parseLongList(e.getMcpIdsJson()));
        vo.setSkillIds(AgentJson.parseLongList(e.getSkillIdsJson()));
        vo.setWorkflowIds(AgentJson.parseLongList(e.getWorkflowIdsJson()));
        vo.setKnowledgeIds(AgentJson.parseLongList(e.getKnowledgeIdsJson()));
        vo.setEmbeddingProviderId(e.getEmbeddingProviderId());
        vo.setEmbeddingModel(e.getEmbeddingModel() == null ? "" : e.getEmbeddingModel());
        vo.setRerankProviderId(e.getRerankProviderId());
        vo.setRerankModel(e.getRerankModel() == null ? "" : e.getRerankModel());
        vo.setRagTopK(e.getRagTopK() == null || e.getRagTopK() < 1 ? AgentConsts.DEFAULT_TOP_K : e.getRagTopK());
        vo.setRagRecallTopK(e.getRagRecallTopK() == null || e.getRagRecallTopK() < 0 ? 0 : e.getRagRecallTopK());
        vo.setRagScoreThreshold(e.getRagScoreThreshold() == null
                ? AgentConsts.DEFAULT_SCORE_THRESHOLD : e.getRagScoreThreshold());
        vo.setFileEnabled(e.getFileEnabled() == null ? 0 : e.getFileEnabled());
        vo.setStatus(e.getStatus());
        vo.setRemark(e.getRemark());
        vo.setVersion(e.getVersion());
        vo.setDraftVersion(e.getDraftVersion());
        vo.setHasUnpublished(hasUnpublished(e));
        vo.setPublicEnabled(e.getPublicEnabled() == null ? 0 : e.getPublicEnabled());
        vo.setPublicToken(e.getPublicToken() == null ? "" : e.getPublicToken());
        vo.setCreateTime(e.getCreateTime());
        vo.setUpdateTime(e.getUpdateTime());
        return vo;
    }

    /** 有未发布改动：草稿版本号高于已发布版本号 */
    private static boolean hasUnpublished(AgentAgent e) {
        return e.getDraftVersion() != null && e.getVersion() != null && e.getDraftVersion() > e.getVersion();
    }

    /** 草稿配置 JSON；为空（未回填的旧数据）时从主表正式字段构建 */
    private static String draftJsonOf(AgentAgent e) {
        if (AgentTexts.notBlank(e.getDraftJson())) {
            return e.getDraftJson();
        }
        AgentSaveDTO dto = new AgentSaveDTO();
        dto.setSystemPrompt(e.getSystemPrompt());
        dto.setProviderId(e.getProviderId());
        dto.setModelCode(e.getModelCode());
        dto.setTemperature(e.getTemperature());
        dto.setTopP(e.getTopP());
        dto.setMaxTokens(e.getMaxTokens());
        dto.setMemoryRounds(e.getMemoryRounds());
        dto.setToolIds(AgentJson.parseLongList(e.getToolIdsJson()));
        dto.setMcpIds(AgentJson.parseLongList(e.getMcpIdsJson()));
        dto.setSkillIds(AgentJson.parseLongList(e.getSkillIdsJson()));
        dto.setWorkflowIds(AgentJson.parseLongList(e.getWorkflowIdsJson()));
        dto.setKnowledgeIds(AgentJson.parseLongList(e.getKnowledgeIdsJson()));
        dto.setEmbeddingProviderId(e.getEmbeddingProviderId());
        dto.setEmbeddingModel(e.getEmbeddingModel());
        dto.setRerankProviderId(e.getRerankProviderId());
        dto.setRerankModel(e.getRerankModel());
        dto.setRagTopK(e.getRagTopK());
        dto.setRagRecallTopK(e.getRagRecallTopK());
        dto.setRagScoreThreshold(e.getRagScoreThreshold());
        dto.setFileEnabled(e.getFileEnabled());
        return AgentJson.toJson(dto);
    }

    /** 把草稿配置应用到主表正式字段（发布用）：只覆盖配置内容，不动基本信息 */
    private static void applyConfig(AgentAgent e, AgentSaveDTO dto) {
        e.setSystemPrompt(dto.getSystemPrompt() == null ? "" : dto.getSystemPrompt());
        e.setProviderId(dto.getProviderId());
        e.setModelCode(dto.getModelCode() == null ? "" : dto.getModelCode());
        e.setTemperature(dto.getTemperature() == null ? AgentConsts.DEFAULT_TEMPERATURE : dto.getTemperature());
        e.setTopP(dto.getTopP() == null ? BigDecimal.ONE : dto.getTopP());
        e.setMaxTokens(dto.getMaxTokens() != null && dto.getMaxTokens() > 0 ? dto.getMaxTokens() : 0);
        e.setMemoryRounds(dto.getMemoryRounds());
        e.setToolIdsJson(AgentJson.toJson(dto.getToolIds() == null ? List.of() : dto.getToolIds()));
        e.setMcpIdsJson(AgentJson.toJson(dto.getMcpIds() == null ? List.of() : dto.getMcpIds()));
        e.setSkillIdsJson(AgentJson.toJson(dto.getSkillIds() == null ? List.of() : dto.getSkillIds()));
        e.setWorkflowIdsJson(AgentJson.toJson(dto.getWorkflowIds() == null ? List.of() : dto.getWorkflowIds()));
        e.setKnowledgeIdsJson(AgentJson.toJson(dto.getKnowledgeIds() == null ? List.of() : dto.getKnowledgeIds()));
        e.setEmbeddingProviderId(dto.getEmbeddingProviderId());
        e.setEmbeddingModel(dto.getEmbeddingModel() == null ? "" : dto.getEmbeddingModel().trim());
        e.setRerankProviderId(dto.getRerankProviderId());
        e.setRerankModel(dto.getRerankModel() == null ? "" : dto.getRerankModel().trim());
        e.setRagTopK(dto.getRagTopK() == null || dto.getRagTopK() < 1 ? AgentConsts.DEFAULT_TOP_K : Math.min(AgentConsts.MAX_TOP_K, dto.getRagTopK()));
        e.setRagRecallTopK(normalizeRecallTopK(dto.getRagRecallTopK()));
        e.setRagScoreThreshold(dto.getRagScoreThreshold() == null
                ? AgentConsts.DEFAULT_SCORE_THRESHOLD : dto.getRagScoreThreshold());
        e.setFileEnabled(dto.getFileEnabled() == null ? 0 : (dto.getFileEnabled() == 1 ? 1 : 0));
    }

    /** 把草稿配置覆盖到 VO（编辑页回显用） */
    private static void applyDraftToVo(AgentVO vo, AgentSaveDTO draft) {
        vo.setSystemPrompt(draft.getSystemPrompt() == null ? "" : draft.getSystemPrompt());
        vo.setProviderId(draft.getProviderId());
        vo.setModelCode(draft.getModelCode() == null ? "" : draft.getModelCode());
        vo.setTemperature(draft.getTemperature());
        vo.setTopP(draft.getTopP());
        vo.setMaxTokens(draft.getMaxTokens());
        vo.setMemoryRounds(draft.getMemoryRounds());
        vo.setToolIds(draft.getToolIds() == null ? List.of() : draft.getToolIds());
        vo.setMcpIds(draft.getMcpIds() == null ? List.of() : draft.getMcpIds());
        vo.setSkillIds(draft.getSkillIds() == null ? List.of() : draft.getSkillIds());
        vo.setWorkflowIds(draft.getWorkflowIds() == null ? List.of() : draft.getWorkflowIds());
        vo.setKnowledgeIds(draft.getKnowledgeIds() == null ? List.of() : draft.getKnowledgeIds());
        vo.setEmbeddingProviderId(draft.getEmbeddingProviderId());
        vo.setEmbeddingModel(draft.getEmbeddingModel() == null ? "" : draft.getEmbeddingModel());
        vo.setRerankProviderId(draft.getRerankProviderId());
        vo.setRerankModel(draft.getRerankModel() == null ? "" : draft.getRerankModel());
        vo.setRagTopK(draft.getRagTopK());
        vo.setRagRecallTopK(draft.getRagRecallTopK() == null || draft.getRagRecallTopK() < 0 ? 0 : draft.getRagRecallTopK());
        vo.setRagScoreThreshold(draft.getRagScoreThreshold());
        vo.setFileEnabled(draft.getFileEnabled() == null ? 0 : (draft.getFileEnabled() == 1 ? 1 : 0));
    }

    private AgentVersionVO toVersionVo(AgentAgentVersion e) {
        AgentVersionVO vo = new AgentVersionVO();
        vo.setId(e.getId());
        vo.setAgentId(e.getAgentId());
        vo.setVersion(e.getVersion());
        vo.setName(e.getName());
        vo.setCode(e.getCode());
        vo.setStatus(e.getStatus());
        vo.setRemark(e.getRemark());
        vo.setPublishedBy(e.getPublishedBy());
        vo.setPublishedAt(e.getPublishedAt());
        return vo;
    }

    private Map<Long, String> loadProviderNames(List<AgentAgent> agents) {
        List<Long> ids = agents.stream()
                .map(AgentAgent::getProviderId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return providerMapper.selectByIds(ids).stream()
                .collect(Collectors.toMap(AgentProvider::getId, AgentProvider::getName, (a, b) -> a, HashMap::new));
    }

    /** null → 默认轮数；上限见 {@link AgentConsts#MAX_MEMORY_ROUNDS}；允许 0（关闭记忆）。 */
    private static int normalizeMemoryRounds(Integer v) {
        if (v == null) {
            return AgentConsts.DEFAULT_MEMORY_ROUNDS;
        }
        if (v < 0) {
            return 0;
        }
        return Math.min(AgentConsts.MAX_MEMORY_ROUNDS, v);
    }
}
