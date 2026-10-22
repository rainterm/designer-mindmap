package com.designer.admin.system.constant;

/**
 * 登录尝试（验证码阈值与账号锁定）的默认值。
 *
 * <p>真正的阈值由参数配置下发，这里只放「配置缺失时用什么」以及「计数键最少活多久」。
 * 后者是防止验证码阈值提前失效的下限：失败计数 TTL 必须覆盖锁定时长，
 * 否则锁定还没结束、失败次数已经被 Redis 清掉，验证码就会提前「放过」攻击者。</p>
 *
 * @author 雨夜终端
 */
public final class LoginAttemptConsts {

    /** 失败计数 TTL 下限（秒）：30 分钟 */
    public static final int FAIL_COUNT_MIN_TTL_SECONDS = 1800;

    /** 锁定时长与失败计数 TTL 之间的换算：分钟 → 秒 */
    public static final int SECONDS_PER_MINUTE = 60;

    private LoginAttemptConsts() {
    }
}
