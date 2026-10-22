package com.designer.admin.module.agent.model.dto.provider;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.noear.solon.validation.annotation.Length;

/**
 * 拉取供应商模型列表参数
 *
 * @author 雨夜终端
 */
@Data
public class ProviderFetchModelsDTO {

    @ApiModelProperty("供应商 ID（编辑场景；apiKey 为空时复用库中密钥）")
    private Long id;

    @ApiModelProperty("Solon 方言：openai / ollama / anthropic / gemini / dashscope 等")
    @Length(max = 64, message = "类型长度不能超过 64 个字符")
    private String type;

    @ApiModelProperty("API Base URL")
    @Length(max = 500, message = "API Base URL 长度不能超过 500 个字符")
    private String baseUrl;

    @ApiModelProperty("API Key")
    @Length(max = 500, message = "API Key 长度不能超过 500 个字符")
    private String apiKey;
}
