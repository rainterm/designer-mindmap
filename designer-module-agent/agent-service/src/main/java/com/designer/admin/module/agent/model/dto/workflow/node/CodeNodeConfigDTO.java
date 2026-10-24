package com.designer.admin.module.agent.model.dto.workflow.node;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 代码节点配置。
 *
 * @author 雨夜终端
 */
@Data
public class CodeNodeConfigDTO {

    @ApiModelProperty("语言：js / python，默认 js")
    private String language;

    @ApiModelProperty("脚本正文")
    private String script;
}
