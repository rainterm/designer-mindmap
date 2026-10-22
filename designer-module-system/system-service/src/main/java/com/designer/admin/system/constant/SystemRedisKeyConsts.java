package com.designer.admin.system.constant;

/**
 * 系统模块的 Redis 键（前缀）。
 *
 * <p>键名是跨版本的数据契约：写侧与读侧必须引用同一个常量，不允许在业务类里再写一份字面量，
 * 否则改前缀时漏掉一处，就会出现「写得进去、读不出来」的静默故障。
 * 这里只放系统模块自己拥有的键；限流等框架级键见 {@code framework.redis.RedisKeyConsts}。</p>
 *
 * @author 雨夜终端
 */
public final class SystemRedisKeyConsts {

    /**
     * 账号 + IP 维度的登录失败计数：{@code login:pair:fail:{ip}:{username}}，值为失败次数。
     *
     * <p>IP 放在用户名之前，是为了让「用户名里带冒号」也不会产生键歧义：
     * IP 本身不含冒号，按第一个冒号切开就能唯一还原出 IP 与用户名。</p>
     */
    public static final String LOGIN_PAIR_FAIL_PREFIX = "login:pair:fail:";

    /**
     * 账号 + IP 维度的锁定标记：{@code login:pair:lock:{ip}:{username}}，存在即锁定，TTL 为剩余时长。
     *
     * <p><b>锁定必须带 IP</b>：只按用户名锁定等于给攻击者一个 DoS 开关 ——
     * 随便输几次错密码就能把别人的账号锁在门外。带上 IP 后，攻击者只能锁住
     * 「自己这个来源 + 目标账号」的组合，受害者换回自己的网络照常登录。</p>
     */
    public static final String LOGIN_PAIR_LOCK_PREFIX = "login:pair:lock:";

    /**
     * 账号维度的失败计数：{@code login:acct:fail:{username}}，不区分来源 IP。
     *
     * <p>只用于决定「要不要出人机验证」，<b>绝不据此锁定</b>：
     * 分布式撞库会让这个计数涨得很快，若它触发锁定，攻击者花少量 IP 就能永久锁死目标账号。</p>
     */
    public static final String LOGIN_ACCOUNT_FAIL_PREFIX = "login:acct:fail:";

    /** IP 维度的失败计数：{@code login:ip:fail:{ip}}，不区分账号，用于识别撞库 / 密码喷洒 */
    public static final String LOGIN_IP_FAIL_PREFIX = "login:ip:fail:";

    /** IP 锁定标记：{@code login:ip:lock:{ip}}，存在即锁定，TTL 为剩余时长 */
    public static final String LOGIN_IP_LOCK_PREFIX = "login:ip:lock:";

    /** SSO 一次性票据：{@code sso:ticket:{ticket}}，值为签发方的 Sa-Token */
    public static final String SSO_TICKET_PREFIX = "sso:ticket:";

    /** 参数配置缓存：{@code sys:config:{configKey}}，值为参数值 */
    public static final String CONFIG_CACHE_PREFIX = "sys:config:";

    /**
     * Sa-Token 会话键的固定后缀。
     *
     * <p>完整键由 Sa-Token 决定：{@code {tokenName}:{loginType}:session:{loginId}}。
     * 前缀部分只能在运行时拼（token 名与登录类型都可能被配置改），
     * 这里固定住后缀，避免在线用户列表靠硬编码的 {@code ":session:"} 反推。</p>
     */
    public static final String SA_TOKEN_SESSION_SUFFIX = ":session:";

    private SystemRedisKeyConsts() {
    }
}
