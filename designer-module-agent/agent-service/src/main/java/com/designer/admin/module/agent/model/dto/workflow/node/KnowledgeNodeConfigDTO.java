package com.designer.admin.module.agent.model.dto.workflow.node;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 知识库检索节点配置。
 *
 * @author 雨夜终端
 */
@Data
public class KnowledgeNodeConfigDTO {

    @ApiModelProperty("知识库 ID")
    private Long knowledgeId;

    @ApiModelProperty("知识库编码（未填 ID 时按编码查找）")
    private String knowledgeCode;

    @ApiModelProperty("查询词模板")
    private String query;

    @ApiModelProperty("召回条数 Top K")
    private Integer topK;

    @ApiModelProperty("相似度阈值 0~1")
    private Double scoreThreshold;
}
