package com.designer.admin.module.agent.model.dto.workflow.node;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * LLM 节点配置。
 *
 * @author 雨夜终端
 */
@Data
public class LlmNodeConfigDTO {

    @ApiModelProperty("模型供应商 ID")
    private Long providerId;

    @ApiModelProperty("模型 code")
    private String modelCode;

    @ApiModelProperty("温度")
    private Double temperature;

    @ApiModelProperty("Top P")
    private Double topP;

    @ApiModelProperty("最大 token 数")
    private Integer maxTokens;

    @ApiModelProperty("是否把模型输出推送到回答流")
    private Boolean emitAnswer;

    @ApiModelProperty("是否流式输出")
    private Boolean stream;

    @ApiModelProperty("是否推送思考过程")
    private Boolean emitThinking;

    @ApiModelProperty("绑定 Skill code 列表")
    private List<String> skillCodes;

    @ApiModelProperty("绑定内置工具 code 列表")
    private List<String> toolCodes;

    @ApiModelProperty("绑定 MCP code 列表")
    private List<String> mcpCodes;

    @ApiModelProperty("系统提示词模板")
    private String systemPrompt;

    @ApiModelProperty("用户提示词模板")
    private String userPrompt;
}
