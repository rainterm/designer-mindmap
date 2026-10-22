package com.designer.admin.module.agent.model.vo.workflow;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 工作流开始节点入参定义。
 *
 * @author 雨夜终端
 */
@Data
public class WorkflowStartInputVO {

    @ApiModelProperty("参数名（写入请求体 params）")
    private String key;

    @ApiModelProperty("显示名称")
    private String label;

    @ApiModelProperty("是否必填")
    private Boolean required;

    @ApiModelProperty("默认值")
    private String defaultValue;
}
