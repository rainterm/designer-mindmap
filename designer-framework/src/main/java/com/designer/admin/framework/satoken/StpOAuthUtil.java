package com.designer.admin.framework.satoken;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;

import java.util.Collections;
import java.util.List;

/**
 * OAuth2 客户端凭证登录（独立 loginType，与后台用户会话隔离）。
 * <p>Sa-Token 禁止 loginId 含冒号，故直接以 clientId 作为 loginId（已由 loginType=oauth2 隔离）。
 *
 * @author 雨夜终端
 */
public final class StpOAuthUtil {

    /** Authorization 头的 Bearer 前缀 */
    private static final String BEARER_PREFIX = "Bearer ";

    public static final String TYPE = "oauth2";
    public static final String SCOPES_KEY = "scopes";

    public static final StpLogic STP = new StpLogic(TYPE);

    private StpOAuthUtil() {
    }

    public static void login(String clientId, List<String> scopes, long timeoutSeconds) {
        SaLoginParameter param = new SaLoginParameter().setTimeout(timeoutSeconds);
        // 客户端凭证不按活跃续期踢出
        param.setActiveTimeout(-1);
        STP.login(clientId, param);
        STP.getSession().set(SCOPES_KEY, scopes == null ? Collections.emptyList() : List.copyOf(scopes));
    }

    public static void checkLogin() {
        STP.checkLogin();
    }

    /**
     * 从 Authorization 头校验（兼容 {@code Bearer xxx} 与裸 token）。
     * 显式写入请求级 Storage，避免仅改 header 时 Sa-Token 读不到。
     */
    public static void checkLoginByAuthorization(String authorization) {
        String token = stripBearer(authorization);
        if (token == null || token.isBlank()) {
            throw NotLoginException.newInstance(TYPE, NotLoginException.NOT_TOKEN, NotLoginException.NOT_TOKEN_MESSAGE, null);
        }
        STP.setTokenValueToStorage(token);
        STP.checkLogin();
    }

    public static String getTokenValue() {
        return STP.getTokenValue();
    }

    public static String getClientId() {
        Object id = STP.getLoginIdDefaultNull();
        return id == null ? null : id.toString();
    }

    @SuppressWarnings("unchecked")
    public static List<String> getScopes() {
        Object v = STP.getSession().get(SCOPES_KEY);
        if (v instanceof List<?> list) {
            return (List<String>) list;
        }
        return Collections.emptyList();
    }

    /** 去掉 Bearer 前缀，便于与标准 OAuth2 客户端兼容 */
    public static String stripBearer(String authorization) {
        if (authorization == null) {
            return null;
        }
        String v = authorization.trim();
        int prefixLength = BEARER_PREFIX.length();
        if (v.length() > prefixLength && v.regionMatches(true, 0, BEARER_PREFIX, 0, prefixLength)) {
            return v.substring(prefixLength).trim();
        }
        return v;
    }
}
