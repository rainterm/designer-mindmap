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
import org.noear.solon.validation.annotation.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * 知识库保存参数
 *
 * @author 雨夜终端
 */
@Data
public class KnowledgeSaveDTO {

    @ApiModelProperty("主键 ID，新增时留空")
    private Long id;

    @ApiModelProperty(value = "名称", required = true)
    @NotBlank(message = "知识库名称不能为空")
    @Length(max = 100, message = "知识库名称长度不能超过 100 个字符")
    private String name;

    @ApiModelProperty("编码（留空时由服务端生成）")
    @Length(max = 64, message = "知识库编码长度不能超过 64 个字符")
    private String code;

    @ApiModelProperty("描述")
    @Length(max = 500, message = "描述长度不能超过 500 个字符")
    private String description;

    @ApiModelProperty("标签列表")
    private List<String> tags;

    @ApiModelProperty(value = "Embedding 供应商 ID", required = true)
    @NotNull(message = "请选择 Embedding 供应商")
    private Long embeddingProviderId;

    @ApiModelProperty(value = "Embedding 模型", required = true)
    @NotBlank(message = "请选择 Embedding 模型")
    @Length(max = 128, message = "Embedding 模型长度不能超过 128 个字符")
    private String embeddingModel;

    @ApiModelProperty("分块策略：fixed / section_recursive")
    @Length(max = 32, message = "分块策略长度不能超过 32 个字符")
    private String chunkStrategy;

    @ApiModelProperty("分块大小")
    @Min(value = AgentConsts.MIN_CHUNK_SIZE, message = "分块大小不能小于 " + AgentConsts.MIN_CHUNK_SIZE)
    @Max(value = 8000, message = "分块大小不能超过 8000")
    private Integer chunkSize;

    @ApiModelProperty("分块重叠")
    @Min(value = 0, message = "分块重叠不能为负")
    @Max(value = 2000, message = "分块重叠不能超过 2000")
    private Integer chunkOverlap;

    @ApiModelProperty("Top K")
    @Min(value = 1, message = "Top K 至少为 1")
    @Max(value = AgentConsts.MAX_TOP_K, message = "Top K 不能超过 " + AgentConsts.MAX_TOP_K)
    private Integer topK;

    @ApiModelProperty("相似度阈值")
    @DecimalMin(value = 0d, message = "相似度阈值不能小于 0")
    @DecimalMax(value = 1d, message = "相似度阈值不能大于 1")
    private BigDecimal scoreThreshold;

    @ApiModelProperty("状态：1启用 0停用")
    private Integer status;
}
