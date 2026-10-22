package com.designer.admin.module.agent.util;

/**
 * 字符串判断公共方法，避免各 Service 复制 notBlank。
 *
 * @author 雨夜终端
 */
public final class AgentTexts {

    private AgentTexts() {
    }

    public static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    public static String trimOrEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    public static String trimOrDefault(String s, String def) {
        return notBlank(s) ? s.trim() : def;
    }
}
