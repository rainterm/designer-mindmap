package com.designer.admin.framework.satoken;

import cn.dev33.satoken.stp.StpUtil;

/**
 * 当前操作者标识：后台用户名、OAuth 客户端或公开访问 token。
 *
 * <p>用于 {@code create_by} 自动填充与会话归属隔离。
 * 公开归属串需落在常见 {@code create_by VARCHAR(50)} 长度内。</p>
 *
 * @author 雨夜终端
 */
public final class OperatorHelper {

    /** OAuth 客户端凭证前缀 */
    public static final String OAUTH_PREFIX = "oauth:";

    /**
     * 公开访问前缀（短写，避免 {@code public:token:visitor} 超出 create_by 长度）。
     * 形如 {@code p:{token}} / {@code p:{token}:{visitor8}}。
     */
    public static final String PUBLIC_PREFIX = "p:";

    /** create_by 常见列宽上限 */
    private static final int MAX_OPERATOR_LEN = 50;

    private OperatorHelper() {
    }

    /**
     * 解析当前操作者。
     *
     * @return 后台用户名，或 {@code oauth:{clientId}} / {@code p:{token}}；无身份时返回空串
     */
    public static String currentOperator() {
        try {
            if (StpUtil.isLogin()) {
                String username = LoginHelper.getUsername();
                if (username != null && !username.isBlank()) {
                    return fit(username);
                }
            }
        } catch (Exception ignored) {
            // 非请求线程或无用户会话
        }
        try {
            if (StpOAuthUtil.STP.isLogin()) {
                String clientId = StpOAuthUtil.getClientId();
                if (clientId != null && !clientId.isBlank()) {
                    return fit(OAUTH_PREFIX + clientId);
                }
            }
        } catch (Exception ignored) {
            // OAuth 上下文不可用
        }
        return "";
    }

    /**
     * 公开访问场景的操作者标识。
     *
     * @param publicToken 公开 token
     * @return {@code p:{token}}
     */
    public static String publicOperator(String publicToken) {
        return publicOperator(publicToken, null);
    }

    /**
     * 公开访问场景的操作者标识（按访客隔离会话）。
     *
     * @param publicToken 公开 token
     * @param visitorId   浏览器侧访客 ID，可空
     * @return {@code p:{token}} 或 {@code p:{token}:{visitor8}}，长度不超过 50
     */
    public static String publicOperator(String publicToken, String visitorId) {
        if (publicToken == null || publicToken.isBlank()) {
            return "";
        }
        // token 截到 32，与 UUID 去横线长度一致，保证 p:{32} 仅 34 字符
        String token = publicToken.trim().replaceAll("[^a-zA-Z0-9]", "");
        if (token.length() > 32) {
            token = token.substring(0, 32);
        }
        String base = PUBLIC_PREFIX + token;
        if (visitorId == null || visitorId.isBlank()) {
            return fit(base);
        }
        String vid = visitorId.trim().replaceAll("[^a-zA-Z0-9]", "");
        if (vid.isEmpty()) {
            return fit(base);
        }
        // 访客只取 8 位，整串 p:{32}:{8} = 43
        if (vid.length() > 8) {
            vid = vid.substring(0, 8);
        }
        return fit(base + ":" + vid);
    }

    private static String fit(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value.length() <= MAX_OPERATOR_LEN ? value : value.substring(0, MAX_OPERATOR_LEN);
    }
}
