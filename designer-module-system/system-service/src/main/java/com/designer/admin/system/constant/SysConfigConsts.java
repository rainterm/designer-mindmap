package com.designer.admin.system.constant;

/**
 * 参数配置（{@code sys_config}）的键名、内置类型与默认值。
 *
 * <p>键名既是数据库里的数据，也是代码里的读取入口：{@code SysConfigServiceImpl} 按它读、
 * 登录策略按它取默认值、SQL 初始化脚本按它建行。以前这些常量挂在 {@code SysConfigService}
 * 接口上，实现类还要靠「实现接口」拿到裸名字，接口因此背上了与抽象无关的数据字典职责。
 * 现在统一收在这里，接口只留方法。</p>
 *
 * <p><b>键名一旦发布就不允许改</b>：库里已有按旧键名保存的行，改常量等于让配置静默失效。</p>
 *
 * @author 雨夜终端
 */
public final class SysConfigConsts {

    /** 是否启用滑块验证码 */
    public static final String KEY_CAPTCHA_ENABLED = "sys.account.captchaEnabled";

    /** 连续登录失败达到该次数后才要求验证码 */
    public static final String KEY_CAPTCHA_FAIL_THRESHOLD = "sys.account.captchaFailThreshold";

    /** 是否启用账号锁定 */
    public static final String KEY_LOCK_ENABLED = "sys.account.lockEnabled";

    /** 触发锁定所需的连续失败次数 */
    public static final String KEY_LOCK_MAX_RETRY = "sys.account.lockMaxRetry";

    /** 锁定时长（分钟） */
    public static final String KEY_LOCK_MINUTES = "sys.account.lockMinutes";

    /** 同一 IP 累计失败达到该次数后锁定该 IP（跨账号，用于拦撞库 / 密码喷洒） */
    public static final String KEY_LOCK_IP_MAX_RETRY = "sys.account.lockIpMaxRetry";

    /** IP 锁定时长（分钟） */
    public static final String KEY_LOCK_IP_MINUTES = "sys.account.lockIpMinutes";

    /** Token 绝对有效期（秒），到期必须重新登录 */
    public static final String KEY_TOKEN_TIMEOUT = "sys.account.tokenTimeout";

    /** Token 活跃续期窗口（秒），有请求则续期；-1 关闭 */
    public static final String KEY_TOKEN_ACTIVE_TIMEOUT = "sys.account.tokenActiveTimeout";

    /** Token 绝对有效期默认值（秒）：1 天 */
    public static final int DEFAULT_TOKEN_TIMEOUT = 86400;

    /** Token 活跃续期默认值（秒）：30 分钟 */
    public static final int DEFAULT_TOKEN_ACTIVE_TIMEOUT = 1800;

    /** Token 有效期下限（秒）：低于该值视为配置错误，回退默认值 */
    public static final int MIN_TOKEN_SECONDS = 60;

    /** 连续失败次数默认阈值 */
    public static final int DEFAULT_CAPTCHA_FAIL_THRESHOLD = 3;

    /** 连续失败次数默认上限（未配置 {@link #KEY_LOCK_MAX_RETRY} 时） */
    public static final int DEFAULT_LOCK_MAX_RETRY = 5;

    /** 锁定时长默认值（分钟，未配置 {@link #KEY_LOCK_MINUTES} 时） */
    public static final int DEFAULT_LOCK_MINUTES = 10;

    /** 同一 IP 失败次数默认上限（未配置 {@link #KEY_LOCK_IP_MAX_RETRY} 时） */
    public static final int DEFAULT_LOCK_IP_MAX_RETRY = 30;

    /** IP 锁定时长默认值（分钟，未配置 {@link #KEY_LOCK_IP_MINUTES} 时） */
    public static final int DEFAULT_LOCK_IP_MINUTES = 10;

    /** 锁定时长上限（分钟）：防止配置成天文数字后把用户长期锁在门外 */
    public static final int MAX_LOCK_MINUTES = 24 * 60;

    /** 配置类型：内置（不允许删除） */
    public static final String TYPE_BUILTIN = "Y";

    /** 配置类型：普通 */
    public static final String TYPE_NORMAL = "N";

    /** 参数值缓存存活时间（秒）：1 小时 */
    public static final int CACHE_SECONDS = 3600;

    private SysConfigConsts() {
    }
}
