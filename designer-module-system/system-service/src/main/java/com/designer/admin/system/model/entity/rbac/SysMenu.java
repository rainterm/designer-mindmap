package com.designer.admin.system.model.entity.rbac;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.designer.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 系统菜单（含目录 M / 菜单 C / 按钮 F 三种类型，按钮码即权限标识）
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends BaseEntity {

    /** 父菜单ID，0 表示顶级 */
    private Long parentId;

    /** 菜单名称 */
    private String menuName;

    /** 菜单类型：M目录 / C菜单 / F按钮 */
    private String menuType;

    /** 路由地址（前端 path） */
    private String path;

    /** 前端组件路径；目录固定为 Layout */
    private String component;

    /** 权限标识，如 {@code system:user:add}；仅按钮类型使用 */
    private String perms;

    /** 图标（前端图标组件名） */
    private String icon;

    /** 显示顺序，升序 */
    private Integer sort;

    /** 是否显示：1显示 0隐藏 */
    private Integer visible;

    /** 1正常 0停用 */
    private Integer status;

    /** 子节点，非数据库字段 */
    @TableField(exist = false)
    private List<SysMenu> children = new ArrayList<>();
}