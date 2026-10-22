package com.designer.admin.system.constant;

/**
 * User-Agent 解析常量：登录日志与在线用户列表里的浏览器 / 操作系统标识。
 *
 * <p><b>为什么不散在 service 里</b>：这组值既用于「匹配 UA 特征串」，又用于「写入数据库的展示文案」，
 * 两件事混在一处最容易改一半漏一半 —— 例如把浏览器显示名改成「谷歌浏览器」却忘了改匹配串。
 * 集中在这里，匹配串与展示名相邻声明，改的时候一眼能对上。</p>
 *
 * <p><b>匹配顺序有依赖</b>（见 {@code AuthServiceImpl#parseBrowser} /
 * {@code AuthServiceImpl#parseOs}）：</p>
 * <ul>
 *   <li>{@link #TOKEN_EDGE} 必须早于 {@link #TOKEN_CHROME}：Edge 的 UA 同时含 {@code Chrome/}，
 *       反过来判断会把 Edge 认成 Chrome；</li>
 *   <li>{@link #TOKEN_SAFARI} 必须最后判断：Chrome / Edge 的 UA 也含 {@code Safari/}；</li>
 *   <li>{@link #TOKEN_ANDROID} 早于 {@link #TOKEN_LINUX}：Android UA 里带 {@code Linux}。</li>
 * </ul>
 *
 * @author 雨夜终端
 */
public final class UserAgentConsts {

    /** 拿不到 UA 时写入的兜底值 */
    public static final String UNKNOWN = "未知";

    /** UA 存在但不属于已知浏览器 / 操作系统时的兜底值 */
    public static final String OTHER = "其它";

    /** UA 特征串：Edge（须先于 Chrome 判断） */
    public static final String TOKEN_EDGE = "Edg/";

    /** UA 特征串：Chrome */
    public static final String TOKEN_CHROME = "Chrome/";

    /** UA 特征串：Firefox */
    public static final String TOKEN_FIREFOX = "Firefox/";

    /** UA 特征串：Safari（须最后判断） */
    public static final String TOKEN_SAFARI = "Safari/";

    /** UA 特征串：Windows */
    public static final String TOKEN_WINDOWS = "Windows";

    /** UA 特征串：macOS（新版本写法） */
    public static final String TOKEN_MAC_OS_X = "Mac OS X";

    /** UA 特征串：macOS（老版本写法） */
    public static final String TOKEN_MACINTOSH = "Macintosh";

    /** UA 特征串：Android（须先于 Linux 判断） */
    public static final String TOKEN_ANDROID = "Android";

    /** UA 特征串：iPhone */
    public static final String TOKEN_IPHONE = "iPhone";

    /** UA 特征串：iPad */
    public static final String TOKEN_IPAD = "iPad";

    /** UA 特征串：Linux */
    public static final String TOKEN_LINUX = "Linux";

    /** 浏览器标识：Edge */
    public static final String BROWSER_EDGE = "Edge";

    /** 浏览器标识：Chrome */
    public static final String BROWSER_CHROME = "Chrome";

    /** 浏览器标识：Firefox */
    public static final String BROWSER_FIREFOX = "Firefox";

    /** 浏览器标识：Safari */
    public static final String BROWSER_SAFARI = "Safari";

    /** 操作系统标识：Windows */
    public static final String OS_WINDOWS = "Windows";

    /** 操作系统标识：macOS */
    public static final String OS_MACOS = "macOS";

    /** 操作系统标识：Android */
    public static final String OS_ANDROID = "Android";

    /** 操作系统标识：iOS */
    public static final String OS_IOS = "iOS";

    /** 操作系统标识：Linux */
    public static final String OS_LINUX = "Linux";

    private UserAgentConsts() {
    }
}
