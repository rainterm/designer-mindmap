package com.designer.admin.module.agent.service.agent;

import com.designer.admin.module.agent.model.dto.agent.AgentQueryDTO;
import com.designer.admin.module.agent.model.dto.agent.AgentSaveDTO;
import com.designer.admin.module.agent.model.entity.agent.AgentAgent;
import com.designer.admin.module.agent.model.vo.agent.AgentVO;
import com.designer.admin.module.agent.model.vo.agent.AgentVersionVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import java.util.List;

/**
 * 智能体管理
 *
 * @author 雨夜终端
 */
public interface AgentAgentService {

    /**
     * 智能体分页（管理端，含草稿与未发布）。
     *
     * @param query 分页与名称 / 编码 / 状态筛选条件
     * @return 智能体分页结果
     */
    IPage<AgentVO> page(AgentQueryDTO query);

    /**
     * 商店分页：仅「已启用且已发布」的智能体，供使用者浏览使用。
     *
     * @param query 分页与名称 / 编码筛选条件
     * @return 智能体分页结果
     */
    IPage<AgentVO> pageStore(AgentQueryDTO query);

    /**
     * 已启用智能体列表（下拉等场景）。
     *
     * @return 已启用智能体列表
     */
    List<AgentVO> listEnabled();

    /**
     * 已启用且已发布的智能体列表（开放接口）。
     *
     * @return 已发布智能体列表
     */
    List<AgentVO> listPublished();

    /**
     * 编辑页详情：面向草稿（draft_json 优先）；运行时请用 {@link #requireEntity(Long)} 读已发布字段。
     *
     * @param id 智能体 ID
     * @return 智能体详情（含草稿配置）
     */
    AgentVO getById(Long id);

    /**
     * 已发布详情（开放接口 / 公开页）：不套草稿；未启用或从未发布时抛错。
     *
     * @param id 智能体 ID
     * @return 已发布配置详情
     */
    AgentVO getPublishedById(Long id);

    /**
     * 新增智能体。
     *
     * @param dto 保存参数
     * @return 新建后的智能体
     */
    AgentVO save(AgentSaveDTO dto);

    /**
     * 保存草稿：基本信息（名称/图标/状态/备注）保存即生效；
     * 配置内容（提示词/模型/采样/能力绑定/RAG）只写 draft_json，不动主表正式字段 —— 保存不影响运行。
     *
     * @param dto 保存参数
     * @return 更新后的智能体
     */
    AgentVO update(AgentSaveDTO dto);

    /**
     * 发布：把草稿（draft_json）固化为已发布配置（主表正式字段），写一份快照到历史表，version+1。
     * 对话运行读主表正式字段，因此发布前可以多次保存草稿而不影响运行。
     *
     * @param id 智能体 ID
     * @return 发布后的智能体
     */
    AgentVO publish(Long id);

    /**
     * 历史快照分页列表（按 version 倒序）。
     *
     * @param id       智能体 ID
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return 版本历史分页结果
     */
    IPage<AgentVersionVO> listHistory(Long id, long pageNum, long pageSize);

    /**
     * 取指定历史快照的完整配置（用于回滚预览/查看）。
     *
     * @param id      智能体 ID
     * @param version 历史版本号
     * @return 指定版本的完整配置
     */
    AgentVersionVO getVersion(Long id, Integer version);

    /**
     * 回滚到指定历史版本：仅把快照配置覆盖到草稿（draft_json）。
     * 不动基本信息、不动已发布配置、不动 version。
     *
     * @param id            智能体 ID
     * @param targetVersion 目标历史版本号
     * @return 回滚后的智能体（草稿已覆盖）
     */
    AgentVO rollback(Long id, Integer targetVersion);

    /**
     * 批量删除智能体。
     *
     * @param ids 智能体 ID 列表
     */
    void remove(List<Long> ids);

    /**
     * 按主键加载已发布实体，不存在时抛出业务异常。
     *
     * @param id 智能体 ID
     * @return 智能体实体（已发布字段）
     */
    AgentAgent requireEntity(Long id);

    /**
     * 把草稿配置（draft_json）覆盖到实体副本的运行字段（提示词/模型/采样/能力绑定/RAG）。
     * 仅用于调试对话等「试运行」场景：只改内存对象，调用方不得 updateById 持久化。
     *
     * @param agent 智能体实体副本
     */
    void applyDraftConfig(AgentAgent agent);

    /**
     * 开启公开访问链接（已启用且已发布才可）。
     *
     * @param id 智能体 ID
     * @return 更新后的智能体（含 publicToken）
     */
    AgentVO enablePublic(Long id);

    /**
     * 关闭公开访问并清空 token（旧链接立即失效）。
     *
     * @param id 智能体 ID
     * @return 更新后的智能体
     */
    AgentVO disablePublic(Long id);

    /**
     * 轮换公开 token（旧链接立即失效）。
     *
     * @param id 智能体 ID
     * @return 更新后的智能体
     */
    AgentVO rotatePublic(Long id);

    /**
     * 按公开 token 加载已启用且已公开的智能体。
     *
     * @param publicToken 公开 token
     * @return 智能体实体
     */
    AgentAgent requireByPublicToken(String publicToken);

}
