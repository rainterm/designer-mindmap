package com.designer.admin.module.agent.model.dto.agent;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.noear.solon.validation.annotation.Min;
import org.noear.solon.validation.annotation.NotNull;

/**
 * 智能体回滚参数
 *
 * @author 雨夜终端
 */
@Data
public class AgentRollbackDTO {

    @ApiModelProperty(value = "目标历史版本号", required = true)
    @NotNull(message = "版本号不能为空")
    @Min(value = 1, message = "版本号无效")
    private Integer version;
}
