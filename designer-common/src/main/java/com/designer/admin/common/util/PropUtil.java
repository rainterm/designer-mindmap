package com.designer.admin.common.util;

import java.util.Map;

/**
 * 配置 Map（库表 configJson 解析后的 props）取值工具。
 *
 * <p>对象存储与消息通道的运行时配置都是 {@code Map<String, Object>}：前端的
 * {@code "true"} / {@code "1"}、数字字符串与真正的 JSON 布尔 / 数字混在一起。
 * 各处自己写 {@code String.valueOf} + {@code Integer.parseInt}，容错口径迟早不一致
 * （例如一边把空串当默认值、另一边当成 0）。这里统一：
 * JSON 类型直接取值，字符串去空白后解析，解析失败退回默认值。</p>
 *
 * @author 雨夜终端
 */
public final class PropUtil {

    private PropUtil() {
    }

    /** 取字符串；键不存在 / 值为 null 时返回空串 */
    public static String getString(Map<String, Object> props, String key) {
        Object value = value(props, key);
        return value == null ? "" : String.valueOf(value);
    }

    /** 取字符串；键不存在 / 值为 null / 值为空白时返回默认值 */
    public static String getString(Map<String, Object> props, String key, String defaultValue) {
        String value = getString(props, key);
        return value.isBlank() ? defaultValue : value;
    }

    /** 取布尔；认 JSON 布尔与 {@code "true"} / {@code "1"}（忽略大小写），其余为 {@code false} */
    public static boolean getBool(Map<String, Object> props, String key, boolean defaultValue) {
        Object value = value(props, key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean b) {
            return b;
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(text) || "1".equals(text);
    }

    /** 取整数；解析失败返回默认值 */
    public static int getInt(Map<String, Object> props, String key, int defaultValue) {
        Object value = value(props, key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /** 取长整数；解析失败返回默认值 */
    public static long getLong(Map<String, Object> props, String key, long defaultValue) {
        Object value = value(props, key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static Object value(Map<String, Object> props, String key) {
        return props == null || key == null ? null : props.get(key);
    }
}
