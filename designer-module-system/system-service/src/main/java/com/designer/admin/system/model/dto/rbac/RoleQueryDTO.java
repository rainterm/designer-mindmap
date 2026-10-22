package com.designer.admin.system.model.dto.rbac;

import com.designer.admin.common.core.PageQuery;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色查询条件
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RoleQueryDTO extends PageQuery {

    @ApiModelProperty("角色名称，模糊匹配")
    private String roleName;

    @ApiModelProperty("角色标识，模糊匹配")
    private String roleKey;

    @ApiModelProperty("状态：1启用 0停用，为空表示不限")
    private Integer status;
}
