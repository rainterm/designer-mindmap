package com.designer.admin.module.agent.model.dto.workflow;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.noear.solon.validation.annotation.Min;
import org.noear.solon.validation.annotation.NotNull;

/**
 * 工作流回滚参数
 *
 * @author 雨夜终端
 */
@Data
public class WorkflowRollbackDTO {

    @ApiModelProperty(value = "目标历史版本号", required = true)
    @NotNull(message = "版本号不能为空")
    @Min(value = 1, message = "版本号无效")
    private Integer version;
}
