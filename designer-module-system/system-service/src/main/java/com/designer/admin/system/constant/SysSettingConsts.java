package com.designer.admin.system.constant;

/**
 * 系统设置（{@code sys_setting}，单行表）的默认值与取值范围。
 *
 * <p>这些默认值会同时出现在两处：设置行缺失时的兜底读取，以及「前端把字段清空」时的归一化回填。
 * 两处各写一份字面量，改默认主题色时漏掉一处，就会出现「首次打开是这个颜色、清空后是另一个颜色」。
 * 因此统一放在这里。</p>
 *
 * @author 雨夜终端
 */
public final class SysSettingConsts {

    /** 单行表的固定主键 */
    public static final long SINGLETON_ID = 1L;

    /** 默认系统名称 */
    public static final String DEFAULT_SYSTEM_NAME = "Designer Admin";

    /** 默认登录页副标题 */
    public static final String DEFAULT_LOGIN_SUBTITLE = "统一管理后台，让协作更高效";

    /** 默认布局模式（与前端 constants/layout.js 的 DEFAULT_LAYOUT_MODE 保持一致） */
    public static final String DEFAULT_LAYOUT_MODE = "mixed-vertical";

    /** 默认主题色（与前端 theme.js 的 colors.primary 保持一致） */
    public static final String DEFAULT_PRIMARY_COLOR = "#16A34A";

    /** Logo 圆角默认值（px） */
    public static final int DEFAULT_LOGO_RADIUS = 6;

    /** Logo 圆角上限（px），超过视为脏值 */
    public static final int MAX_LOGO_RADIUS = 100;

    private SysSettingConsts() {
    }
}
