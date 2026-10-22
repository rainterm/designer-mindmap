package com.designer.admin.common.constant;

/**
 * HTTP / URL 相关的通用字面量。
 *
 * <p>这些值同时出现在菜单外链、友链、站点 Logo、SSO 回跳等完全不相干的地方。
 * 各自写一份 {@code "http://"} 的后果不是编译错误，而是拼错一个字符后
 * 「看起来像 URL、实际判不出来」——例如写成 {@code "http:;//"} 仍然能编译、能运行，
 * 只在特定数据上静默走错分支。集中声明后，判断与拼接永远一致。</p>
 *
 * @author 雨夜终端
 */
public final class HttpConsts {

    /** 协议名：http（不含冒号与斜杠，用于 URI 解析后的 scheme 比较） */
    public static final String SCHEME_HTTP = "http";

    /** 协议名：https */
    public static final String SCHEME_HTTPS = "https";

    /** URL 前缀：http:// */
    public static final String PREFIX_HTTP = "http://";

    /** URL 前缀：https:// */
    public static final String PREFIX_HTTPS = "https://";

    /** 路径分隔符 */
    public static final String PATH_SEPARATOR = "/";

    private HttpConsts() {
    }
}
