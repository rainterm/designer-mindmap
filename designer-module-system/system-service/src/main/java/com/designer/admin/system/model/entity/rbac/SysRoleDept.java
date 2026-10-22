package com.designer.admin.system.model.entity.rbac;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 角色数据权限 - 部门关联表（联合主键：role_id + dept_id，无自增主键）。
 *
 * <p>只在角色的 {@code data_scope = 2（自定义部门）} 时有意义；
 * 其它数据范围下本表记录会被清空 —— 避免「先切成自定义勾了部门、又切回本部门」
 * 之后残留一批看不见的授权，下次再切回自定义时凭空多出部门。</p>
 *
 * @author 雨夜终端
 */
@Data
@TableName("sys_role_dept")
public class SysRoleDept implements Serializable {

    /** 角色ID */
    @TableField("role_id")
    private Long roleId;

    /** 部门ID */
    @TableField("dept_id")
    private Long deptId;
}
