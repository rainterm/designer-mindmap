package com.designer.admin.system.service.auth;

/**
 * 登录失败计数与账号锁定，以及「本次登录是否需要人机验证」的判据。
 *
 * <p><b>为什么不叫 CaptchaService</b>：这个类原先叫 CaptchaService，把三件事混在一起 ——
 * 画算术题验证码、累计失败次数、锁定账号。验证码形态换成滑块后
 * （AJ-Captcha，见 {@code com.designer.admin.framework.captcha}），画图那部分整体删除：
 * 底图与拼图由插件现生成，凭据校验交给 {@code CaptchaVerifier}。
 * 唯一与验证码形态无关、因而必须留下的是<b>策略</b>——
 * 「这个账号失败够次数了吗，要不要拦一道人机验证」，以及配套的计数与锁定。
 * 于是改名为此，职责收窄为「登录尝试」本身。</p>
 *
 * <p><b>与 {@code CaptchaVerifier} 的分工</b>：这里只回答「要不要验」，
 * 「凭据是否有效」由 {@code CaptchaVerifier} 回答。调用方（{@code AuthService}）
 * 先问这里，需要验再去调那边，顺序不能反 —— 凭据是一次性的，
 * 对不需要验证的账号调一次校验只会把它白白消费掉。</p>
 *
 * @author 雨夜终端
 */
public interface LoginAttemptService {

    /**
     * 是否需要校验人机验证：全局开启且该账号失败次数达到阈值。
     *
     * <p>阈值看的是<b>账号维度</b>（不区分来源 IP）的连续失败次数，
     * 这样分布式撞库也会被要求过滑块；但账号维度只影响「要不要验」，不参与锁定，
     * 避免攻击者用少量 IP 把目标账号永久锁死（锁定看账号 + IP 组合，见 {@link #assertNotLocked}）。</p>
     *
     * <p>滑块本身不需要「按账号生成」，所以这里只回答是 / 否，
     * 前端据此决定要不要调 {@code /app-api/captcha/get} 取一张。</p>
     *
     * @param username 登录账号
     * @return 是否需要滑块验证
     */
    boolean isCaptchaRequired(String username);

    /**
     * 查询账号当前的连续登录失败次数（账号维度，不区分来源 IP）。
     *
     * @param username 登录账号
     * @return 失败次数；账号为空或计数异常时返回 0
     */
    long countLoginFail(String username);

    /**
     * 校验「账号 + 来源 IP」是否处于锁定期，锁定中直接抛出业务异常并提示剩余时间。
     *
     * <p>先查账号 + IP 组合，再查 IP 维度。两者都不存在「只按账号」的锁：
     * 攻击者输错几次密码最多锁住自己这个来源，不会影响账号真正主人从别处登录。</p>
     *
     * @param username 登录账号
     * @param ip       归一化后的客户端 IP；为空或 {@code unknown} 时跳过 IP 维度（无法归属来源，不误伤他人）
     */
    void assertNotLocked(String username, String ip);

    /**
     * 记录一次登录失败：累计账号维度次数（供验证码阈值）与账号 + IP 维度次数，达到上限则锁定。
     *
     * @param username 登录账号
     * @param ip       归一化后的客户端 IP
     * @return 账号 + IP 维度当前失败次数
     */
    long saveLoginFail(String username, String ip);

    /**
     * 登录成功后清空该账号的失败计数与锁定标记（账号维度 + 当前 IP 的账号组合）。
     *
     * <p>特意<b>不清 IP 维度</b>：否则攻击者只要拥有任意一个自己的账号，
     * 成功登录一次就能把整台机器的失败计数清零，撞库成本立刻归零。</p>
     *
     * @param username 登录账号
     * @param ip       归一化后的客户端 IP
     */
    void removeLoginFail(String username, String ip);
}
