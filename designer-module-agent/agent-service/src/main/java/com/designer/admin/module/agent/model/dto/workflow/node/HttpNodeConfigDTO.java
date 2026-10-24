package com.designer.admin.module.agent.model.dto.workflow.node;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * HTTP 节点配置。
 *
 * @author 雨夜终端
 */
@Data
public class HttpNodeConfigDTO {

    @ApiModelProperty("HTTP 方法，默认 GET")
    private String method;

    @ApiModelProperty("请求 URL")
    private String url;

    @ApiModelProperty("超时秒数")
    private Integer timeoutSec;

    @ApiModelProperty("请求体类型：json / form / multipart / raw / none")
    private String bodyType;

    @ApiModelProperty("原始请求体或兼容多行 key=value")
    private String body;

    @ApiModelProperty("旧版多行请求头文本（Key: Value）")
    private String headers;

    @ApiModelProperty("请求头表单字段")
    private List<HttpKeyValueDTO> headerFields;

    @ApiModelProperty("表单字段")
    private List<HttpKeyValueDTO> formFields;
}
