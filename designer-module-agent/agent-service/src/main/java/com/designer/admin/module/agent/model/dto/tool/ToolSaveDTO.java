package com.designer.admin.module.agent.model.dto.tool;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.noear.solon.validation.annotation.NotNull;

/**
 * 工具保存参数
 *
 * @author 雨夜终端
 */
@Data
public class ToolSaveDTO {

    @ApiModelProperty(value = "工具 ID", required = true)
    @NotNull(message = "工具ID不能为空")
    private Long id;

    @ApiModelProperty(value = "状态：1启用 0停用（内置工具仅允许改状态）", required = true)
    @NotNull(message = "状态不能为空")
    private Integer status;
}
