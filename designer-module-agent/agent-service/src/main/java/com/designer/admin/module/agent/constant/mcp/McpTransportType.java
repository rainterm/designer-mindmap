package com.designer.admin.module.agent.constant.mcp;

/**
 * MCP 传输方式（落库 {@code agent_mcp.transport}）。
 *
 * @author 雨夜终端
 */
public enum McpTransportType {

    /** 本地 stdio */
    STDIO("stdio"),
    /** SSE */
    SSE("sse"),
    /** Streamable 无状态 */
    STREAMABLE_STATELESS("streamable_stateless"),
    /** HTTP（归一化为 streamable） */
    HTTP("http"),
    /** Streamable HTTP */
    STREAMABLE("streamable"),
    /** Streamable HTTP 别名 */
    STREAMABLE_HTTP("streamable_http");

    private final String code;

    McpTransportType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean eq(String value) {
        return code.equals(value);
    }

    public static McpTransportType fromCode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String t = value.trim().toLowerCase();
        for (McpTransportType type : values()) {
            if (type.code.equals(t)) {
                return type;
            }
        }
        return null;
    }

    /** 是否走 SSE / Streamable 类远程传输。 */
    public boolean isRemoteStream() {
        return this == SSE || this == STREAMABLE || this == STREAMABLE_STATELESS;
    }
}
