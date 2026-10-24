package com.designer.admin.module.agent.model.dto.workflow.node;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 输出节点配置。
 *
 * @author 雨夜终端
 */
@Data
public class OutputNodeConfigDTO {

    @ApiModelProperty("输出类型：text / table，默认 text")
    private String type;

    @ApiModelProperty("输出模板")
    private String template;
}
