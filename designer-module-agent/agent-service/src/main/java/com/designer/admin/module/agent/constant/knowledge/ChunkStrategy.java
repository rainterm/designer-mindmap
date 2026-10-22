package com.designer.admin.module.agent.constant.knowledge;

import java.util.Locale;

/**
 * 知识库文本分块策略。
 *
 * @author 雨夜终端
 */
public enum ChunkStrategy {

    /** 固定窗口 */
    FIXED("fixed"),
    /** 按章节递归 */
    SECTION_RECURSIVE("section_recursive");

    private final String code;

    ChunkStrategy(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean eq(String value) {
        return code.equals(value);
    }

    /** 非法或空值回退为章节递归。 */
    public static String normalize(String strategy) {
        if (strategy == null || strategy.isBlank()) {
            return SECTION_RECURSIVE.code;
        }
        String s = strategy.trim().toLowerCase(Locale.ROOT);
        for (ChunkStrategy cs : values()) {
            if (cs.code.equals(s)) {
                return cs.code;
            }
        }
        return SECTION_RECURSIVE.code;
    }
}
