package com.designer.admin.system.service.auth.impl;

import com.designer.admin.common.exception.BizException;
import com.designer.admin.framework.client.ClientIpUtil;
import com.designer.admin.framework.redis.RedisCounter;
import com.designer.admin.system.constant.LoginAttemptConsts;
import com.designer.admin.system.constant.SysConfigConsts;
import com.designer.admin.system.constant.SystemRedisKeyConsts;
import com.designer.admin.system.service.auth.LoginAttemptService;
import com.designer.admin.system.service.config.SysConfigService;
import org.noear.redisx.RedisClient;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import java.util.Locale;

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
 * <p><b>三个维度，各管一段</b>（键定义见 {@link SystemRedisKeyConsts}）：</p>
 * <ul>
 *   <li><b>账号 + IP</b>：达到 {@code lockMaxRetry} 就锁定该组合。这是唯一的锁定判据，
 *       攻击者只能锁住自己的来源，受害者换回自己的网络即可登录 —— 从根上消掉
 *       「随便输错几次就能锁死别人账号」的 DoS；</li>
 *   <li><b>账号</b>（跨 IP）：只用于决定要不要出滑块，<b>不锁定</b>。
 *       分布式撞库会把计数推高，但代价只是受害者多滑一次，账号不会被锁死；</li>
 *   <li><b>IP</b>（跨账号）：累计失败达到 {@code lockIpMaxRetry} 就临时锁定该 IP，
 *       拦住「一个密码试一百个账号」的密码喷洒。阈值取得比单账号高，
 *       避免 NAT / 办公网共用出口时被个别用户输错密码连坐。</li>
 * </ul>
 *
 * @author 雨夜终端
 */
@Component
public class LoginAttemptServiceImpl implements LoginAttemptService {

    @Inject
    private RedisClient redisClient;
    @Inject
    private RedisCounter redisCounter;
    @Inject
    private SysConfigService configService;

    @Override
    public boolean isCaptchaRequired(String username) {
        if (!configService.getBool(SysConfigConsts.KEY_CAPTCHA_ENABLED, true)) {
            return false;
        }
        return countLoginFail(username) >= captchaFailThreshold();
    }

    private int captchaFailThreshold() {
        int threshold = configService.getInt(
                SysConfigConsts.KEY_CAPTCHA_FAIL_THRESHOLD, SysConfigConsts.DEFAULT_CAPTCHA_FAIL_THRESHOLD);
        return Math.max(1, threshold);
    }

    @Override
    public long countLoginFail(String username) {
        String user = normalizeUsername(username);
        if (user == null) {
            return 0;
        }
        String failKey = SystemRedisKeyConsts.LOGIN_ACCOUNT_FAIL_PREFIX + user;
        String val = redisClient.openAndGet(s -> s.key(failKey).get());
        if (val == null || val.isBlank()) {
            return 0;
        }
        try {
            return Long.parseLong(val.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public void assertNotLocked(String username, String ip) {
        if (!configService.getBool(SysConfigConsts.KEY_LOCK_ENABLED, true)) {
            return;
        }
        String user = normalizeUsername(username);
        if (user == null) {
            return;
        }
        String clientIp = normalizeIp(ip);
        if (clientIp == null) {
            // 来源无法归属时不按 IP 锁定：兜底键被所有无 IP 的请求共用，
            // 一旦有人把它刷到阈值，会误伤全部同类请求
            return;
        }

        String pairLockKey = pairLockKey(clientIp, user);
        if (exists(pairLockKey)) {
            throw new BizException("登录失败次数过多，账号已锁定，请 " + remainingMinutes(pairLockKey) + " 分钟后再试");
        }
        String ipLockKey = SystemRedisKeyConsts.LOGIN_IP_LOCK_PREFIX + clientIp;
        if (exists(ipLockKey)) {
            throw new BizException("当前网络登录失败次数过多，请 " + remainingMinutes(ipLockKey) + " 分钟后再试");
        }
    }

    @Override
    public long saveLoginFail(String username, String ip) {
        String user = normalizeUsername(username);
        if (user == null) {
            return 0;
        }
        boolean captchaEnabled = configService.getBool(SysConfigConsts.KEY_CAPTCHA_ENABLED, true);
        boolean lockEnabled = configService.getBool(SysConfigConsts.KEY_LOCK_ENABLED, true);
        if (!captchaEnabled && !lockEnabled) {
            return 0;
        }

        long accountCount = 0;
        if (captchaEnabled) {
            // 账号维度：只给验证码阈值用，永远不据此锁定
            accountCount = redisCounter.incrWithTtl(
                    SystemRedisKeyConsts.LOGIN_ACCOUNT_FAIL_PREFIX + user, failTtl());
        }

        String clientIp = normalizeIp(ip);
        if (!lockEnabled || clientIp == null) {
            return accountCount;
        }

        int maxRetry = configService.getInt(
                SysConfigConsts.KEY_LOCK_MAX_RETRY, SysConfigConsts.DEFAULT_LOCK_MAX_RETRY);
        maxRetry = Math.max(1, maxRetry);
        int lockMinutes = lockMinutes(SysConfigConsts.KEY_LOCK_MINUTES, SysConfigConsts.DEFAULT_LOCK_MINUTES);

        String pairFailKey = pairFailKey(clientIp, user);
        long pairCount = redisCounter.incrWithTtl(pairFailKey, failTtl());
        if (pairCount >= maxRetry) {
            redisClient.open(s -> {
                s.key(pairLockKey(clientIp, user)).expire(lockMinutes * LoginAttemptConsts.SECONDS_PER_MINUTE).set("1");
                s.key(pairFailKey).delete();
            });
            throw new BizException("登录失败次数过多，账号已锁定 " + lockMinutes + " 分钟");
        }

        // 同一个密码试多个账号（密码喷洒）：按 IP 汇总，与具体账号无关
        int ipMaxRetry = configService.getInt(
                SysConfigConsts.KEY_LOCK_IP_MAX_RETRY, SysConfigConsts.DEFAULT_LOCK_IP_MAX_RETRY);
        ipMaxRetry = Math.max(1, ipMaxRetry);
        int ipLockMinutes = lockMinutes(SysConfigConsts.KEY_LOCK_IP_MINUTES, SysConfigConsts.DEFAULT_LOCK_IP_MINUTES);

        String ipFailKey = SystemRedisKeyConsts.LOGIN_IP_FAIL_PREFIX + clientIp;
        long ipCount = redisCounter.incrWithTtl(ipFailKey, failTtl(ipLockMinutes));
        if (ipCount >= ipMaxRetry) {
            redisClient.open(s -> {
                s.key(SystemRedisKeyConsts.LOGIN_IP_LOCK_PREFIX + clientIp)
                        .expire(ipLockMinutes * LoginAttemptConsts.SECONDS_PER_MINUTE).set("1");
                s.key(ipFailKey).delete();
            });
            throw new BizException("当前网络登录失败次数过多，请 " + ipLockMinutes + " 分钟后再试");
        }
        return pairCount;
    }

    @Override
    public void removeLoginFail(String username, String ip) {
        String user = normalizeUsername(username);
        if (user == null) {
            return;
        }
        String clientIp = normalizeIp(ip);
        redisClient.open(s -> {
            s.key(SystemRedisKeyConsts.LOGIN_ACCOUNT_FAIL_PREFIX + user).delete();
            if (clientIp != null) {
                s.key(pairFailKey(clientIp, user)).delete();
                s.key(pairLockKey(clientIp, user)).delete();
            }
            // 故意不删 LOGIN_IP_FAIL_PREFIX / LOGIN_IP_LOCK_PREFIX：
            // 否则攻击者只要有一个自己的合法账号，成功登录一次就能清空整台机器的失败计数
        });
    }

    /**
     * 失败计数 TTL：至少覆盖锁定时长，且不少于 30 分钟。
     *
     * <p>不覆盖锁定时长会出现「锁还没结束、计数先过期」的窗口：解锁瞬间计数从 0 开始，
     * 验证码阈值被提前重置。</p>
     */
    private int failTtl() {
        return failTtl(lockMinutes(SysConfigConsts.KEY_LOCK_MINUTES, SysConfigConsts.DEFAULT_LOCK_MINUTES));
    }

    private int failTtl(int lockMinutes) {
        return Math.max(lockMinutes * LoginAttemptConsts.SECONDS_PER_MINUTE,
                LoginAttemptConsts.FAIL_COUNT_MIN_TTL_SECONDS);
    }

    private int lockMinutes(String configKey, int defaultValue) {
        int minutes = configService.getInt(configKey, defaultValue);
        return Math.min(Math.max(1, minutes), SysConfigConsts.MAX_LOCK_MINUTES);
    }

    private static String pairFailKey(String ip, String user) {
        return SystemRedisKeyConsts.LOGIN_PAIR_FAIL_PREFIX + ip + ":" + user;
    }

    private static String pairLockKey(String ip, String user) {
        return SystemRedisKeyConsts.LOGIN_PAIR_LOCK_PREFIX + ip + ":" + user;
    }

    private static String normalizeUsername(String username) {
        // 与登录入口保持同一口径（去首尾空白 + 转小写），否则 admin / Admin / ADMIN
        // 会各占一个计数键，锁定与验证码阈值都能被大小写变体绕过
        if (username == null) {
            return null;
        }
        String user = username.trim().toLowerCase(Locale.ROOT);
        return user.isEmpty() ? null : user;
    }

    /**
     * 归一化 IP，无法归属来源时返回 {@code null}（调用方据此跳过 IP 维度）。
     *
     * <p>不能用 {@code unknown} 当键：所有解析不到 IP 的请求会共用一个计数器，
     * 攻击者反复触发即可把它刷到阈值，从而锁死所有同类请求。</p>
     */
    private static String normalizeIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return null;
        }
        String value = ip.trim();
        return ClientIpUtil.UNKNOWN_IP.equals(value) ? null : value;
    }

    private boolean exists(String key) {
        return Boolean.TRUE.equals(redisClient.openAndGet(s -> s.key(key).exists()));
    }

    private long remainingMinutes(String lockKey) {
        long ttl = redisClient.openAndGet(s -> s.key(lockKey).ttl());
        return Math.max(1, (ttl + 59) / 60);
    }
}
