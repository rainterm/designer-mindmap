package com.designer.admin.module.agent.service.knowledge;

import java.util.List;

/**
 * 知识库检索结果（注入上下文的文本 + 命中统计）。
 *
 * @author 雨夜终端
 */
public record RagRetrieveResult(String block, int hitCount, List<Hit> hits) {

    public static RagRetrieveResult empty() {
        return new RagRetrieveResult("", 0, List.of());
    }

    public boolean hit() {
        return hitCount > 0;
    }

    public record Hit(
            String kbName,
            long knowledgeId,
            long docId,
            String docTitle,
            int chunkIndex,
            float score,
            String content
    ) {
    }
}
