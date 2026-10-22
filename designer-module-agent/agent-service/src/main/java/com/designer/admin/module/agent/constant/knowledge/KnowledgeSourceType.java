package com.designer.admin.module.agent.constant.knowledge;

/**
 * 知识库文档来源类型。
 *
 * @author 雨夜终端
 */
public enum KnowledgeSourceType {

    /** 上传文件 */
    UPLOAD("upload"),
    /** 纯文本 */
    TEXT("text"),
    /** 网页 URL */
    URL("url");

    private final String code;

    KnowledgeSourceType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean eq(String value) {
        return code.equals(value);
    }

    public static KnowledgeSourceType fromCode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (KnowledgeSourceType t : values()) {
            if (t.code.equalsIgnoreCase(value.trim())) {
                return t;
            }
        }
        return null;
    }
}
