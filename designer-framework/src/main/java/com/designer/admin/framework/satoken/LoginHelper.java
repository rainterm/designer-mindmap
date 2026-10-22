package com.designer.admin.framework.satoken;

import cn.dev33.satoken.stp.StpUtil;

/**
 * 登录用户读取辅助
 *
 * @author 雨夜终端
 */
public final class LoginHelper {

    public static final String LOGIN_USER_KEY = "loginUser";

    private LoginHelper() {
    }

    public static Long getUserId() {
        return Long.valueOf(StpUtil.getLoginId().toString());
    }

    /**
     * 取当前登录用户 ID，未登录或无 Web 上下文时返回 {@code null}。
     *
     * <p>与 {@link #getUserId()} 的区别：那个方法用于「一定在登录态」的接口，
     * 未登录直接抛异常是正确的。数据权限解析则会出现在定时任务、内部调用等场景，
     * 那里没有会话，需要调用方自己决定「无登录态」该怎么处理，
     * 而不是让一个取值方法把任务整体打断。</p>
     */
    public static Long getUserIdOrNull() {
        try {
            Object loginId = StpUtil.getLoginIdDefaultNull();
            return loginId == null ? null : Long.valueOf(loginId.toString());
        } catch (Exception e) {
            // Sa-Token 在非请求线程里可能拿不到上下文；无登录态即视为无用户
            return null;
        }
    }

    public static String getUsername() {
        Object obj = StpUtil.getSession().get(LOGIN_USER_KEY);
        return obj instanceof LoginUser lu ? lu.getUsername() : "";
    }

    /**
     * 取当前登录账号，未登录 / 会话未装载用户信息时返回 {@code null}。
     *
     * <p>数据权限需要把「创建人 = 当前账号」拼进 SQL。这里刻意返回 {@code null} 而不是空串：
     * 空串会拼出 {@code create_by = ''}，正好命中那些由系统（定时任务、初始化脚本）写入的历史脏数据，
     * 把本不该可见的行放出来。调用方拿不到账号时应当<b>不加</b>这个条件。</p>
     */
    public static String getUsernameOrNull() {
        try {
            Object obj = StpUtil.getSession().get(LOGIN_USER_KEY);
            return obj instanceof LoginUser lu ? lu.getUsername() : null;
        } catch (Exception e) {
            // 非请求线程 / 会话缺失：按无账号处理
            return null;
        }
    }
}

