package com.designer.admin.system.constant;

/**
 * SSO（一次性票据换登录态）的固定值。
 *
 * <p>票据默认存活时间很短：票据一旦被记录（代理日志、浏览器历史），
 * 就等于在有效期内留下了一把可用的门钥匙。</p>
 *
 * @author 雨夜终端
 */
public final class SsoConsts {

    /** 配置键：票据有效期（秒），缺省取 {@link #DEFAULT_TICKET_TTL_SECONDS} */
    public static final String KEY_TICKET_TTL = "designer.sso.ticket-ttl";

    /** 票据默认有效期（秒） */
    public static final int DEFAULT_TICKET_TTL_SECONDS = 60;

    /** 票据有效期的下限（秒）：配置过小时不会低于该值 */
    public static final int MIN_TICKET_TTL_SECONDS = 15;

    private SsoConsts() {
    }
}
