package com.designer.admin.module.agent.model.dto.provider;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.noear.solon.validation.annotation.Length;
import org.noear.solon.validation.annotation.NotBlank;

import java.util.List;
import java.util.Map;

/**
 * 模型供应商保存参数
 *
 * @author 雨夜终端
 */
@Data
public class ProviderSaveDTO {

    @ApiModelProperty("供应商 ID，新增时留空")
    private Long id;

    @ApiModelProperty(value = "名称", required = true)
    @NotBlank(message = "名称不能为空")
    @Length(max = 100, message = "名称长度不能超过 100 个字符")
    private String name;

    @ApiModelProperty(value = "Solon 方言：openai / ollama / anthropic / gemini / dashscope 等", required = true)
    @NotBlank(message = "类型不能为空")
    @Length(max = 64, message = "类型长度不能超过 64 个字符")
    private String type;

    @ApiModelProperty(value = "API Base URL", required = true)
    @NotBlank(message = "API Base URL 不能为空")
    @Length(max = 500, message = "API Base URL 长度不能超过 500 个字符")
    private String baseUrl;

    @ApiModelProperty("API Key（明文入参；落库后脱敏展示）")
    @Length(max = 500, message = "API Key 长度不能超过 500 个字符")
    private String apiKey;

    @ApiModelProperty("模型列表")
    private List<Map<String, Object>> models;

    @ApiModelProperty("状态：1启用 0停用")
    private Integer status;

    @ApiModelProperty("备注")
    @Length(max = 500, message = "备注长度不能超过 500 个字符")
    private String remark;
}
