package com.designer.admin.system.model.entity.rbac;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.designer.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 部门（树形组织）
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dept")
public class SysDept extends BaseEntity {

    /** 父部门ID，0 表示顶级 */
    private Long parentId;

    /** 祖级列表，如 0,100,101 */
    private String ancestors;

    /** 部门名称 */
    private String deptName;

    /** 显示顺序 */
    private Integer sort;

    /** 1正常 0停用 */
    private Integer status;

    @TableField(exist = false)
    private List<SysDept> children = new ArrayList<>();
}
