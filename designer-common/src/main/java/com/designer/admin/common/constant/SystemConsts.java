package com.designer.admin.common.constant;

/**
 * 跨模块共享的通用常量：超管标记、角色类型、菜单类型、通用启用状态。
 *
 * <p>以 {@code final class} + 私有构造方法声明，而不是「常量接口」：
 * 接口是用来定义行为契约的，把常量塞进接口会让实现类无意间继承一堆与本类无关的名字，
 * 也挡不住外部 {@code implements SystemConsts}。</p>
 *
 * @author 雨夜终端
 */
public final class SystemConsts {

    /** 是否超级管理员：1是 */
    public static final int IS_SUPER_YES = 1;
    /** 是否超级管理员：0否 */
    public static final int IS_SUPER_NO = 0;

    /** 角色类型：1=系统内置（不可修改/删除） */
    public static final int ROLE_TYPE_BUILTIN = 1;
    /** 角色类型：2=自定义 */
    public static final int ROLE_TYPE_CUSTOM = 2;

    /** 菜单类型：目录 */
    public static final String MENU_TYPE_DIR = "M";
    /** 菜单类型：菜单 */
    public static final String MENU_TYPE_MENU = "C";
    /** 菜单类型：按钮 */
    public static final String MENU_TYPE_BUTTON = "F";

    /** 通用状态：启用 */
    public static final int STATUS_ENABLE = 1;
    /** 通用状态：停用 */
    public static final int STATUS_DISABLE = 0;

    private SystemConsts() {
    }
}
