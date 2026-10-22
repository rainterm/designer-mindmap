package com.designer.admin.module.agent.service.workflow;

import com.designer.admin.module.agent.model.dto.workflow.WorkflowQueryDTO;
import com.designer.admin.module.agent.model.dto.workflow.WorkflowSaveDTO;
import com.designer.admin.module.agent.model.vo.workflow.WorkflowVO;
import com.designer.admin.module.agent.model.vo.workflow.WorkflowVersionVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import java.util.List;
import java.util.Map;
import org.noear.solon.web.sse.SseEmitter;

/**
 * 工作流管理
 *
 * @author 雨夜终端
 */
public interface AgentWorkflowService {

    /**
     * 工作流分页。
     *
     * @param query 分页与名称 / 编码 / 状态筛选条件
     * @return 工作流分页结果
     */
    IPage<WorkflowVO> page(WorkflowQueryDTO query);

    /**
     * 已启用工作流列表（下拉等场景）。
     *
     * @return 已启用工作流列表
     */
    List<WorkflowVO> listEnabled();

    /**
     * 已启用且已发布的工作流列表（开放接口）。
     *
     * @return 已发布工作流列表（不含图）
     */
    List<WorkflowVO> listPublished();

    /**
     * 编辑页详情：面向草稿图；运行/试跑读取已发布 nodes_json。
     *
     * @param id 工作流 ID
     * @return 工作流详情（含草稿）
     */
    WorkflowVO getById(Long id);

    /**
     * 已发布详情（开放接口）：返回正式 nodes/edges，不暴露草稿。
     *
     * @param id 工作流 ID
     * @return 已发布工作流详情
     */
    WorkflowVO getPublishedById(Long id);

    /**
     * 新增工作流。
     *
     * @param dto 保存参数
     * @return 新建后的工作流
     */
    WorkflowVO save(WorkflowSaveDTO dto);

    /**
     * 保存草稿：只更新草稿图（draft_nodes_json/draft_edges_json）与 draftVersion，
     * 不动已发布内容（nodes_json/edges_json）与 version —— 保存不影响运行。
     *
     * @param dto 保存参数
     * @return 更新后的工作流
     */
    WorkflowVO update(WorkflowSaveDTO dto);

    /**
     * 发布：把草稿固化为已发布内容（nodes_json/edges_json），写一份快照到历史表，version+1。
     * 运行/试跑读取 nodes_json，因此发布前可以多次保存草稿而不影响运行。
     *
     * @param id 工作流 ID
     * @return 发布后的工作流
     */
    WorkflowVO publish(Long id);

    /**
     * 历史快照分页列表（按 version 倒序）。
     *
     * @param id       工作流 ID
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return 版本历史分页结果
     */
    IPage<WorkflowVersionVO> listHistory(Long id, long pageNum, long pageSize);

    /**
     * 取指定历史快照的完整节点/边（用于回滚预览/查看）。
     *
     * @param id      工作流 ID
     * @param version 历史版本号
     * @return 指定版本的完整节点/边
     */
    WorkflowVersionVO getVersion(Long id, Integer version);

    /**
     * 回滚到指定历史版本：仅把快照的节点/边图覆盖到草稿列。
     * 不动基本信息（名称/编码/描述/状态）、不动已发布内容、不动 version。
     *
     * @param id            工作流 ID
     * @param targetVersion 目标历史版本号
     * @return 回滚后的工作流（草稿已覆盖）
     */
    WorkflowVO rollback(Long id, Integer targetVersion);

    /**
     * 批量删除工作流。
     *
     * @param ids 工作流 ID 列表
     */
    void remove(List<Long> ids);

    /**
     * SSE 试跑：event = status / step / delta / thinking / done / error（优先草稿图）。
     *
     * @param id      工作流 ID
     * @param params  开始节点参数，可空
     * @param agentId 可选：借用智能体模型，可空
     * @return SSE 发射器
     */
    SseEmitter runStream(Long id, Map<String, Object> params, Long agentId);

    /**
     * SSE 执行已发布图（开放接口）。
     *
     * @param id      工作流 ID
     * @param params  开始节点参数，可空
     * @param agentId 可选：借用智能体模型，可空
     * @return SSE 发射器
     */
    SseEmitter runPublishedStream(Long id, Map<String, Object> params, Long agentId);

}
