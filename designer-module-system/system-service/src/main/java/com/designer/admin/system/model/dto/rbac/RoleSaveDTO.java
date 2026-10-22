package com.designer.admin.system.model.dto.rbac;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 角色保存（新增/修改通用）
 *
 * @author 雨夜终端
 */
@Data
public class RoleSaveDTO {

    @ApiModelProperty("角色 ID，新增时留空")
    private Long id;

    @ApiModelProperty("角色名称")
    private String roleName;

    @ApiModelProperty("角色标识，全局唯一")
    private String roleKey;

    @ApiModelProperty("排序值，越小越靠前")
    private Integer sort;

    @ApiModelProperty("状态：1启用 0停用")
    private Integer status;

    @ApiModelProperty("角色类型：1内置 2自定义；创建时由服务端强制为自定义，此字段仅用于回显")
    private Integer roleType;

    @ApiModelProperty("备注")
    private String remark;

    @ApiModelProperty("数据权限：1全部 2自定义部门 3本部门 4本部门及以下 5仅本人；为空表示不修改")
    private Integer dataScope;

    @ApiModelProperty("自定义数据权限的部门 ID（dataScope=2 时必填，其余档位忽略）")
    private List<Long> deptIds;

    @ApiModelProperty("授权的菜单 ID 列表，为空表示不改动")
    private List<Long> menuIds;
}
