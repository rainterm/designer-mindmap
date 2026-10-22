package com.designer.admin.module.agent.model.entity.agent;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.designer.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 智能体实体
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_agent")
public class AgentAgent extends BaseEntity {

    /** 名称 */
    private String name;

    /** 编码 */
    private String code;

    /** 头像地址 */
    private String avatar;

    /** 系统提示词 */
    private String systemPrompt;

    /** 模型供应商 ID */
    private Long providerId;

    /** 模型编码 */
    private String modelCode;

    /** 温度 */
    private BigDecimal temperature;

    /** Top P */
    private BigDecimal topP;

    /** 最大输出 token；0=不限制 */
    private Integer maxTokens;

    /** 对话记忆轮数；0=不带历史 */
    private Integer memoryRounds;

    /** 绑定工具 ID 列表 JSON */
    private String toolIdsJson;

    /** 绑定 MCP ID 列表 JSON */
    private String mcpIdsJson;

    /** 绑定 Skill ID 列表 JSON */
    private String skillIdsJson;

    /** 绑定工作流 ID 列表 JSON */
    private String workflowIdsJson;

    /** 绑定知识库 ID 列表 JSON */
    private String knowledgeIdsJson;

    /** RAG Embedding 供应商 ID（允许清空为 null） */
@TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long embeddingProviderId;

    /** RAG Embedding 模型 */
    private String embeddingModel;

    /** RAG 重排序供应商 ID（允许清空为 null） */
@TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long rerankProviderId;

    /** RAG 重排序模型 */
    private String rerankModel;

    /** RAG 最终 Top K（进入上下文的条数；开启 Rerank 时为重排后截断数） */
    private Integer ragTopK;

    /**
     * 向量召回候选数；0=自动（未开 Rerank 时等于 Top K，开启时约为 Top K × 默认倍数）。
     * 仅在开启 Rerank 时有意义：先召回该数量，再重排截成 {@link #ragTopK}。
     */
    private Integer ragRecallTopK;

    /** RAG 相似度阈值（向量召回阶段） */
    private BigDecimal ragScoreThreshold;

    /** 是否支持聊天文件上传：1是 0否 */
    private Integer fileEnabled;

    /** 状态：1启用 0停用 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 已发布版本号 */
    private Integer version;

    /** 草稿版本号；大于 version 表示有未发布改动 */
    private Integer draftVersion;

    /** 草稿配置快照（完整 AgentSaveDTO JSON）；保存草稿只写此列，运行读主表正式字段 */
    private String draftJson;

    /** 是否发布公开访问链接：1是 0否 */
    private Integer publicEnabled;

    /** 公开访问 token；空表示未发布 */
    private String publicToken;
}
