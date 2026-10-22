package com.designer.admin.system.util;

/**
 * 密码验证工具：密码策略常量与长度 / 复杂度的服务层兜底校验。
 *
 * @author 雨夜终端
 */
public final class PasswordValidatorUtil {

    /** 密码最短长度 */
    private static final int MIN_LENGTH = 6;

    /** 密码最长长度 */
    private static final int MAX_LENGTH = 50;

    /**
     * 长度不合规的提示。
     *
     * <p>服务层兜底校验与前端提示共用这一句；DTO 注解里写的是等价的字面量文案，
     * 调整长度时两边要一起改。</p>
     */
    public static final String LENGTH_MESSAGE = "密码长度需在 " + MIN_LENGTH + "~" + MAX_LENGTH + " 个字符之间";

    /** 复杂度不合规的提示 */
    public static final String COMPLEXITY_MESSAGE = "密码需同时包含大写字母、小写字母、数字和特殊符号";

    /**
     * 复杂度正则：必须同时包含大写字母、小写字母、数字、特殊符号。
     *
     * <p>四个零宽断言逐个要求一类字符。其中「特殊符号」取的是
     * <b>ASCII 可见标点</b>（码点 0x21–0x2F、0x3A–0x40、0x5B–0x60、0x7B–0x7E 四段，
     * 即 POSIX 的 {@code [:punct:]}），有意排除空白与中日韩等非 ASCII 字符 ——
     * 否则一个汉字或一个空格就能凑满「特殊符号」，规则形同虚设。
     * 若将来要放开（例如允许全角符号），改这里即可。</p>
     *
     * <p>Solon 的 {@code PatternValidator} 用 {@code matches()} 全匹配（并显式说明过这是为了
     * 防止无锚点正则被「包含即通过」绕过），所以这里不加 {@code ^$}；
     * 末尾的 {@code .+} 不匹配换行，顺带把含换行的密码挡掉。
     * 自身也用 {@code String#matches} 校验，语义与注解保持一致。</p>
     */
    public static final String STRENGTH_REGEXP = "(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!-/:-@\\[-`{-~]).+";

    /**
     * 校验密码长度是否符合要求。
     *
     * @param rawPassword 明文密码
     * @return 长度在 {@link #MIN_LENGTH}~{@link #MAX_LENGTH} 之间时返回 {@code true}；
     * 为 {@code null} 时返回 {@code false}
     */
    public static boolean isValidLength(String rawPassword) {
        if (rawPassword == null) {
            return false;
        }
        int length = rawPassword.length();
        return length >= MIN_LENGTH && length <= MAX_LENGTH;
    }

    /**
     * 校验密码是否同时包含大写字母、小写字母、数字、特殊符号。
     *
     * @param rawPassword 明文密码
     * @return 四类字符齐全时返回 {@code true}；为 {@code null} 时返回 {@code false}
     */
    public static boolean hasAllCharTypes(String rawPassword) {
        return rawPassword != null && rawPassword.matches(STRENGTH_REGEXP);
    }

    /**
     * 返回首个不满足的要求提示；全部满足返回 {@code null}。
     *
     * <p>一次只报一条而不是把所有问题罗列出来：DTO 注解层本来就是一失败即返回
     * （{@code VALIDATE_ALL} 默认 false），服务层与它保持同一种口径，
     * 前端才不会出现「注解报一条、service 报一长串」的两套体验。</p>
     *
     * @param rawPassword 明文密码
     * @return 首个不满足要求的提示；全部满足时返回 {@code null}
     */
    public static String firstViolation(String rawPassword) {
        if (!isValidLength(rawPassword)) {
            return LENGTH_MESSAGE;
        }
        if (!hasAllCharTypes(rawPassword)) {
            return COMPLEXITY_MESSAGE;
        }
        return null;
    }

    /**
     * 私有构造器：工具类禁止实例化。
     */
    private PasswordValidatorUtil() {
    }
}
