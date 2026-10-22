package com.designer.admin.framework.client;

import org.noear.solon.core.handle.Context;

/**
 * 客户端 IP 解析（框架级单一实现）。
 *
 * <p>真实 IP 的**可信解析**在 {@link TrustedProxyHostUtil}（已在 {@code ClientIpConfig}
 * 里全局替换掉 Solon 默认的 {@code HostUtil}），这里只做「取出来 + 归一化」：
 * 取 {@link Context#realIp()}，多级代理下它是 {@code "client, proxy1, proxy2"}，
 * 只有首段是真实客户端；再按库表列宽截断。</p>
 *
 * <p>放在 {@code framework} 而不是各业务模块：限流键与落库 IP 必须是同一个值，
 * 两处各解析一遍最难排查（同一次请求在业务表与访问日志里记成不同 IP）。</p>
 *
 * @author 雨夜终端
 */
public final class ClientIpUtil {

    /** 取不到 IP 时的兜底值：避免所有无 IP 请求共用一个空键 */
    public static final String UNKNOWN_IP = "unknown";

    /** 与各业务表、访问日志的 IP 列宽一致 */
    private static final int MAX_LENGTH = 64;

    private ClientIpUtil() {
    }

    /** 归一化后的客户端 IP（当前请求上下文不可用时返回 {@value #UNKNOWN_IP}） */
    public static String current() {
        return normalize(Context.current());
    }

    /** 归一化后的客户端 IP */
    public static String normalize(Context ctx) {
        if (ctx == null) {
            return UNKNOWN_IP;
        }
        String ip = ctx.realIp();
        if (ip == null || ip.isBlank()) {
            return UNKNOWN_IP;
        }
        int comma = ip.indexOf(',');
        if (comma > 0) {
            ip = ip.substring(0, comma);
        }
        ip = ip.trim();
        if (ip.isEmpty()) {
            return UNKNOWN_IP;
        }
        return ip.length() > MAX_LENGTH ? ip.substring(0, MAX_LENGTH) : ip;
    }
}
