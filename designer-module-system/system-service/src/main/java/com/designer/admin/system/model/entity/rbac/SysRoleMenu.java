package com.designer.admin.system.model.entity.rbac;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 角色 - 菜单关联表（联合主键：role_id + menu_id，无自增主键）
 *
 * @author 雨夜终端
 */
@Data
@TableName("sys_role_menu")
public class SysRoleMenu implements Serializable {

    /** 角色ID */
    @TableField("role_id")
    private Long roleId;

    /** 菜单ID */
    @TableField("menu_id")
    private Long menuId;
}