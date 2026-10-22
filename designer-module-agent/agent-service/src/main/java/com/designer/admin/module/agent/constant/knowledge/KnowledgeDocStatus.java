package com.designer.admin.module.agent.constant.knowledge;

/**
 * 知识库文档 / 知识库聚合索引状态。
 *
 * @author 雨夜终端
 */
public enum KnowledgeDocStatus {

    /** 待索引 */
    PENDING("pending"),
    /** 已就绪 */
    READY("ready"),
    /** 索引中 */
    INDEXING("indexing"),
    /** 失败 */
    FAILED("failed");

    private final String code;

    KnowledgeDocStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean eq(String value) {
        return code.equals(value);
    }

    public static KnowledgeDocStatus fromCode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (KnowledgeDocStatus s : values()) {
            if (s.code.equalsIgnoreCase(value.trim())) {
                return s;
            }
        }
        return null;
    }
}
