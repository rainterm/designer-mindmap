package com.designer.admin.module.agent.model.dto.knowledge;

import com.designer.admin.module.agent.constant.AgentConsts;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.noear.solon.validation.annotation.DecimalMax;
import org.noear.solon.validation.annotation.DecimalMin;
import org.noear.solon.validation.annotation.Length;
import org.noear.solon.validation.annotation.Max;
import org.noear.solon.validation.annotation.Min;
import org.noear.solon.validation.annotation.NotBlank;

import java.math.BigDecimal;

/**
 * 知识库检索测试参数
 *
 * @author 雨夜终端
 */
@Data
public class KnowledgeRetrieveTestDTO {

    @ApiModelProperty(value = "测试查询文本", required = true)
    @NotBlank(message = "请输入测试查询")
    @Length(max = 2000, message = "测试查询长度不能超过 2000 个字符")
    private String query;

    @ApiModelProperty("最终 Top K，默认 5（开启 Rerank 时为重排后截断数）")
    @Min(value = 1, message = "Top K 至少为 1")
    @Max(value = AgentConsts.MAX_TOP_K, message = "Top K 不能超过 " + AgentConsts.MAX_TOP_K)
    private Integer topK;

    @ApiModelProperty("向量召回候选数；不填或 0=自动（开 Rerank 时约为 TopK×3）")
    @Min(value = 0, message = "召回候选数不能为负")
    @Max(value = AgentConsts.MAX_TOP_K, message = "召回候选数不能超过 " + AgentConsts.MAX_TOP_K)
    private Integer recallTopK;

    @ApiModelProperty("相似度阈值（向量召回），默认 0.35")
    @DecimalMin(value = 0d, message = "相似度阈值不能小于 0")
    @DecimalMax(value = 1d, message = "相似度阈值不能大于 1")
    private BigDecimal scoreThreshold;

    @ApiModelProperty("Rerank 供应商 ID（可选；与 rerankModel 同时填写时启用重排序）")
    private Long rerankProviderId;

    @ApiModelProperty("Rerank 模型编码（可选）")
    @Length(max = 128, message = "Rerank 模型长度不能超过 128 个字符")
    private String rerankModel;
}
