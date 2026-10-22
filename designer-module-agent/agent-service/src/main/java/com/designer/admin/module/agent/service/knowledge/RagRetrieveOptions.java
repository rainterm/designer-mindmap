package com.designer.admin.module.agent.service.knowledge;

import com.designer.admin.module.agent.constant.AgentConsts;
import com.designer.admin.module.agent.model.entity.agent.AgentAgent;

import java.math.BigDecimal;

/**
 * 智能体侧 RAG 检索参数（查询 Embedding / Rerank / TopK / 召回候选 / 阈值）。
 *
 * @author 雨夜终端
 */
public record RagRetrieveOptions(
        Long embeddingProviderId,
        String embeddingModel,
        Long rerankProviderId,
        String rerankModel,
        int topK,
        int recallTopK,
        double scoreThreshold
) {
    public static final int DEFAULT_TOP_K = AgentConsts.DEFAULT_TOP_K;
    public static final double DEFAULT_SCORE_THRESHOLD = AgentConsts.DEFAULT_SCORE_THRESHOLD_D;

    public static RagRetrieveOptions defaults() {
        return new RagRetrieveOptions(null, "", null, "", DEFAULT_TOP_K, 0, DEFAULT_SCORE_THRESHOLD);
    }

    public static RagRetrieveOptions fromAgent(AgentAgent agent) {
        if (agent == null) {
            return defaults();
        }
        int topK = agent.getRagTopK() == null || agent.getRagTopK() < 1
                ? DEFAULT_TOP_K : Math.min(AgentConsts.MAX_TOP_K, agent.getRagTopK());
        int recallTopK = agent.getRagRecallTopK() == null || agent.getRagRecallTopK() < 0
                ? 0 : Math.min(AgentConsts.MAX_TOP_K, agent.getRagRecallTopK());
        double threshold = toDouble(agent.getRagScoreThreshold(), DEFAULT_SCORE_THRESHOLD);
        return new RagRetrieveOptions(
                agent.getEmbeddingProviderId(),
                agent.getEmbeddingModel() == null ? "" : agent.getEmbeddingModel().trim(),
                agent.getRerankProviderId(),
                agent.getRerankModel() == null ? "" : agent.getRerankModel().trim(),
                topK,
                recallTopK,
                threshold);
    }

    public boolean hasQueryEmbedding() {
        return embeddingProviderId != null && embeddingModel != null && !embeddingModel.isBlank();
    }

    public boolean hasRerank() {
        return rerankProviderId != null && rerankModel != null && !rerankModel.isBlank();
    }

    /**
     * 向量召回条数：显式 recallTopK &gt; 0 时用其值（且不少于最终 topK）；
     * 否则未开 Rerank 用 topK，开启 Rerank 用 topK × 默认倍数。
     */
    public int resolveFetchK() {
        int k = Math.max(1, Math.min(AgentConsts.MAX_TOP_K, topK));
        if (recallTopK > 0) {
            return Math.min(AgentConsts.MAX_TOP_K, Math.max(k, recallTopK));
        }
        if (!hasRerank()) {
            return k;
        }
        return Math.min(AgentConsts.MAX_TOP_K, Math.max(k, k * AgentConsts.DEFAULT_RECALL_MULTIPLIER));
    }

    private static double toDouble(BigDecimal v, double def) {
        return v == null ? def : v.doubleValue();
    }
}
