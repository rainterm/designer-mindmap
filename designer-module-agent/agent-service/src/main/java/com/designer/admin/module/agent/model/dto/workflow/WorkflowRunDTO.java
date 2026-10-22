package com.designer.admin.module.agent.model.dto.workflow;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Map;

/**
 * 工作流试跑参数
 *
 * @author 雨夜终端
 */
@Data
public class WorkflowRunDTO {

    @ApiModelProperty("开始节点定义的命名参数")
    private Map<String, Object> params;

    @ApiModelProperty("可选：借用该智能体的供应商与模型")
    private Long agentId;
}
