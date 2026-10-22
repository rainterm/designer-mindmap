package com.designer.admin.module.agent.model.dto.agent;

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
import java.util.List;

/**
 * 智能体保存参数
 *
 * @author 雨夜终端
 */
@Data
public class AgentSaveDTO {

    @ApiModelProperty("智能体 ID，新增时留空")
    private Long id;

    @ApiModelProperty(value = "名称", required = true)
    @NotBlank(message = "智能体名称不能为空")
    @Length(max = 100, message = "智能体名称长度不能超过 100 个字符")
    private String name;

    @ApiModelProperty("编码（留空时由服务端生成）")
    @Length(max = 64, message = "智能体编码长度不能超过 64 个字符")
    private String code;

    @ApiModelProperty("头像地址")
    @Length(max = 500, message = "头像地址长度不能超过 500 个字符")
    private String avatar;

    @ApiModelProperty("系统提示词")
    @Length(max = 20000, message = "系统提示词长度不能超过 20000 个字符")
    private String systemPrompt;

    @ApiModelProperty("模型供应商 ID")
    private Long providerId;

    @ApiModelProperty("模型编码")
    @Length(max = 128, message = "模型编码长度不能超过 128 个字符")
    private String modelCode;

    @ApiModelProperty("温度")
    @DecimalMin(value = 0d, message = "温度不能小于 0")
    @DecimalMax(value = 2d, message = "温度不能大于 2")
    private BigDecimal temperature;

    @ApiModelProperty("Top P")
    @DecimalMin(value = 0d, message = "Top P 不能小于 0")
    @DecimalMax(value = 1d, message = "Top P 不能大于 1")
    private BigDecimal topP;

    @ApiModelProperty("最大输出 token；0=不限制")
    @Min(value = 0, message = "最大输出 token 不能为负")
    private Integer maxTokens;

    @ApiModelProperty("对话记忆轮数；0=不带历史")
    @Min(value = 0, message = "记忆轮数不能为负")
    @Max(value = AgentConsts.MAX_MEMORY_ROUNDS, message = "记忆轮数不能超过 " + AgentConsts.MAX_MEMORY_ROUNDS)
    private Integer memoryRounds;

    @ApiModelProperty("绑定工具 ID 列表")
    private List<Long> toolIds;

    @ApiModelProperty("绑定 MCP ID 列表")
    private List<Long> mcpIds;

    @ApiModelProperty("绑定 Skill ID 列表")
    private List<Long> skillIds;

    @ApiModelProperty("绑定工作流 ID 列表")
    private List<Long> workflowIds;

    @ApiModelProperty("绑定知识库 ID 列表")
    private List<Long> knowledgeIds;

    @ApiModelProperty("RAG Embedding 供应商 ID")
    private Long embeddingProviderId;

    @ApiModelProperty("RAG Embedding 模型")
    @Length(max = 128, message = "Embedding 模型长度不能超过 128 个字符")
    private String embeddingModel;

    @ApiModelProperty("RAG 重排序供应商 ID")
    private Long rerankProviderId;

    @ApiModelProperty("RAG 重排序模型")
    @Length(max = 128, message = "重排序模型长度不能超过 128 个字符")
    private String rerankModel;

    @ApiModelProperty("RAG 最终 Top K（进入上下文条数）")
    @Min(value = 1, message = "RAG Top K 至少为 1")
    @Max(value = AgentConsts.MAX_TOP_K, message = "RAG Top K 不能超过 " + AgentConsts.MAX_TOP_K)
    private Integer ragTopK;

    @ApiModelProperty("向量召回候选数；不填或 0=自动（开 Rerank 时约为 TopK×3）")
    @Min(value = 0, message = "召回候选数不能为负")
    @Max(value = AgentConsts.MAX_TOP_K, message = "召回候选数不能超过 " + AgentConsts.MAX_TOP_K)
    private Integer ragRecallTopK;

    @ApiModelProperty("RAG 相似度阈值（向量召回）")
    @DecimalMin(value = 0d, message = "相似度阈值不能小于 0")
    @DecimalMax(value = 1d, message = "相似度阈值不能大于 1")
    private BigDecimal ragScoreThreshold;

    @ApiModelProperty("是否支持聊天文件上传：1是 0否")
    private Integer fileEnabled;

    @ApiModelProperty("状态：1启用 0停用")
    private Integer status;

    @ApiModelProperty("备注")
    @Length(max = 500, message = "备注长度不能超过 500 个字符")
    private String remark;
}
