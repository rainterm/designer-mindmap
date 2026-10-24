package com.designer.admin.module.agent.model.dto.workflow.node;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * HTTP 节点键值对（请求头 / 表单字段）。
 *
 * @author 雨夜终端
 */
@Data
public class HttpKeyValueDTO {

    @ApiModelProperty("键")
    private String key;

    @ApiModelProperty("值（可含变量占位）")
    private String value;
}
