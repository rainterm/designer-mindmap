package com.designer.admin.framework.ratelimit;

/**
 * 限流维度：决定「按谁计数」，即限流键里的主体部分。
 *
 * <p>取值都保证能取到一个稳定的字符串（{@link RateLimitInterceptor} 负责解析），
 * 所以调用方无需关心 Redis 键怎么拼。</p>
 *
 * @author 雨夜终端
 */
public enum RateLimitDimension {

    /**
     * 按客户端 IP：匿名接口的默认选择。
     *
     * <p>IP 由 {@code TrustedProxyHostUtil} 解析（不可伪造的 socket IP 优先于转发头），
     * 所以伪造 {@code X-Forwarded-For} 换不到新的限流桶。</p>
     */
    IP,

    /**
     * 按登录用户（Sa-Token loginId）。
     *
     * <p>未登录时**退化为按 IP**：若统一落到一个 "匿名" 桶，
     * 所有未登录访客会共享同一份额度，一个人刷满就会挡住其他人。</p>
     */
    USER,

    /**
     * 全局共享一个桶：不区分调用方。
     *
     * <p>用于「保护下游」而不是「限制某人」，例如整站某类昂贵操作的总量上限。</p>
     */
    GLOBAL,

    /**
     * 按请求头取值（配合 {@link RateLimit#headerName()}）。
     *
     * <p>请求头带不到值时整条注解跳过，因此按请求头限流时，
     * 通常要再配一条 {@link #IP} 规则兜底 —— 调用方可以不带头，
     * 否则删掉请求头就等于绕过了限流。</p>
     */
    HEADER
}
