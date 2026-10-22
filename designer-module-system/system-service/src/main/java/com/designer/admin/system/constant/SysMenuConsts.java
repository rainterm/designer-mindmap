package com.designer.admin.system.constant;

/**
 * 菜单路由构建的固定组件名与路径格式。
 *
 * <p>这些值不是给人看的文案，而是**契约**：{@code Layout} / {@code Link} / {@code External}
 * 必须与前端路由解析器的约定逐字一致，改一个字前端就会把目录渲染成空白页。
 * 放在常量类里，避免在方法中部被误当成普通字符串随手改掉。</p>
 *
 * @author 雨夜终端
 */
public final class SysMenuConsts {

    /** 目录型菜单使用的布局组件名 */
    public static final String LAYOUT_COMPONENT = "Layout";

    /** 外链型菜单使用的组件名 */
    public static final String COMPONENT_LINK = "Link";

    /** 外部页面型菜单使用的组件名 */
    public static final String COMPONENT_EXTERNAL = "External";

    /** 去掉路径首尾斜杠的正则 */
    public static final String TRIM_SLASH_REGEX = "^/+|/+$";

    private SysMenuConsts() {
    }
}
