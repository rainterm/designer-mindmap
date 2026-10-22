package com.designer.admin.module.agent.model.entity.knowledge;

import com.baomidou.mybatisplus.annotation.TableName;
import com.designer.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 知识库实体
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_knowledge")
public class AgentKnowledge extends BaseEntity {

    /** 名称 */
    private String name;

    /** 编码 */
    private String code;

    /** 描述 */
    private String description;

    /** 标签 JSON */
    private String tagsJson;

    /** RAG Embedding 供应商 ID */
    private Long embeddingProviderId;

    /** RAG Embedding 模型 */
    private String embeddingModel;

    /** 向量维度，首次索引后锁定 */
    private Integer embeddingDim;

    /** 分块策略：fixed / section_recursive */
    private String chunkStrategy;

    /** 分块大小 */
    private Integer chunkSize;

    /** 分块重叠 */
    private Integer chunkOverlap;

    /** 检索 Top K */
    private Integer topK;

    /** 相似度阈值 */
    private BigDecimal scoreThreshold;

    /** 状态：1启用 0停用 */
    private Integer status;
}
