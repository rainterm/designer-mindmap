package com.designer.admin.module.agent.constant.session;

/**
 * 会话消息角色（落库 {@code agent_message.role}）。
 *
 * @author 雨夜终端
 */
public enum AgentMessageRole {

    /** 用户 */
    USER("user"),
    /** 助手 */
    ASSISTANT("assistant"),
    /** 系统 */
    SYSTEM("system"),
    /** 工具调用 */
    TOOL("tool");

    private final String code;

    AgentMessageRole(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean eq(String value) {
        return code.equals(value);
    }

    public boolean eqIgnoreCase(String value) {
        return value != null && code.equalsIgnoreCase(value);
    }

    public static AgentMessageRole fromCode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (AgentMessageRole role : values()) {
            if (role.code.equalsIgnoreCase(value.trim())) {
                return role;
            }
        }
        return null;
    }
}
