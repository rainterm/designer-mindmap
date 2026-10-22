package com.designer.admin.module.agent.model.dto.tool;

import io.swagger.annotations.ApiModelProperty;
import com.designer.admin.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工具查询参数
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ToolQueryDTO extends PageQuery {

    @ApiModelProperty("名称/编码（模糊）")
    private String name;

    @ApiModelProperty("工具类型：builtin")
    private String type;

    @ApiModelProperty("状态：1启用 0停用")
    private Integer status;
}
