package com.designer.admin.framework.crypto;

import at.favre.lib.crypto.bcrypt.BCrypt;

/**
 * 密码哈希（BCrypt）。
 *
 * <p><b>为什么不用 Sa-Token 的 {@code BCrypt}</b>：{@code cn.dev33.satoken.secure.BCrypt}
 * 自 1.41.0 起被标记为 {@code @Deprecated}，官方说明后续不再维护加密模块，
 * 建议改用其它工具库。这里统一收敛到 {@link BCrypt}（at.favre.lib），
 * 业务代码不再直接依赖具体的哈希实现，将来替换算法也只改这一处。</p>
 *
 * <p><b>与历史数据兼容</b>：两套实现的盐前缀同为 {@code $2a$}，已入库的哈希可以继续校验，
 * 升级不需要重置任何密码。计算强度保持原来的 10，避免升级后登录耗时突变。</p>
 *
 * @author 雨夜终端
 */
public final class PasswordUtil {

    /** BCrypt 计算强度，与 Sa-Token 原默认值保持一致 */
    private static final int COST = 10;

    private PasswordUtil() {
    }

    /**
     * 生成密码哈希。
     *
     * @param rawPassword 明文密码
     * @return {@code $2a$} 前缀的 BCrypt 哈希
     */
    public static String encode(String rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("密码不能为空");
        }
        return BCrypt.withDefaults().hashToString(COST, rawPassword.toCharArray());
    }

    /**
     * 校验明文密码与库中哈希是否匹配。
     *
     * @param rawPassword     明文密码
     * @param encodedPassword 库中的 BCrypt 哈希
     * @return 匹配返回 {@code true}；任一参数为空或哈希格式非法返回 {@code false}
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null || encodedPassword.isBlank()) {
            return false;
        }
        return BCrypt.verifyer().verify(rawPassword.toCharArray(), encodedPassword).verified;
    }
}
