package com.designer.admin.system.model.dto.rbac;

import com.designer.admin.common.core.PageQuery;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 岗位查询条件
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PostQueryDTO extends PageQuery {

    @ApiModelProperty("岗位名称，模糊匹配")
    private String postName;

    @ApiModelProperty("岗位编码，模糊匹配")
    private String postCode;

    @ApiModelProperty("状态：1启用 0停用，为空表示不限")
    private Integer status;
}
