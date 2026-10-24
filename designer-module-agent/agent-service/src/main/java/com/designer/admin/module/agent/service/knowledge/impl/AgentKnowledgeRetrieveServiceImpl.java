package com.designer.admin.module.agent.service.knowledge.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.designer.admin.common.constant.SystemConsts;
import com.designer.admin.module.agent.mapper.knowledge.AgentKnowledgeDocMapper;
import com.designer.admin.module.agent.mapper.knowledge.AgentKnowledgeMapper;
import com.designer.admin.module.agent.mapper.provider.AgentProviderMapper;
import com.designer.admin.module.agent.model.entity.knowledge.AgentKnowledge;
import com.designer.admin.module.agent.model.entity.knowledge.AgentKnowledgeDoc;
import com.designer.admin.module.agent.model.entity.provider.AgentProvider;
import com.designer.admin.module.agent.constant.AgentConsts;
import com.designer.admin.module.agent.service.knowledge.AgentKnowledgeRetrieveService;
import com.designer.admin.module.agent.service.knowledge.RagRetrieveOptions;
import com.designer.admin.module.agent.service.knowledge.RagRetrieveResult;
import com.designer.admin.module.agent.service.knowledge.client.AgentEmbeddingClient;
import com.designer.admin.module.agent.service.knowledge.client.AgentRerankClient;
import com.designer.admin.module.agent.service.knowledge.vector.AgentVectorStore;
import com.designer.admin.module.agent.service.knowledge.vector.AgentVectorStoreManager;

/**
 * 按 query 向量检索知识库分块，支持智能体侧 TopK / 阈值 / Embedding / Rerank。
 *
 * @author 雨夜终端
 */
@Component
public class AgentKnowledgeRetrieveServiceImpl implements AgentKnowledgeRetrieveService {

    private static final Logger log = LoggerFactory.getLogger(AgentKnowledgeRetrieveServiceImpl.class);

    @Db
    private AgentKnowledgeMapper knowledgeMapper;
    @Db
    private AgentKnowledgeDocMapper knowledgeDocMapper;
    @Db
    private AgentProviderMapper providerMapper;
    @Inject
    private AgentEmbeddingClient embeddingClient;
    @Inject
    private AgentRerankClient rerankClient;
    @Inject
    private AgentVectorStoreManager vectorStoreManager;

    @Override
    public String retrieveBlock(List<Long> knowledgeIds, String query, int maxChars) {
        return retrieveDetailed(knowledgeIds, query, maxChars, RagRetrieveOptions.defaults()).block();
    }

    @Override
    public String retrieveBlock(List<Long> knowledgeIds, String query, int maxChars,
                                RagRetrieveOptions options) {
        return retrieveDetailed(knowledgeIds, query, maxChars, options).block();
    }

    @Override
    public RagRetrieveResult retrieveDetailed(List<Long> knowledgeIds, String query, int maxChars,
                                              RagRetrieveOptions options) {
        if (knowledgeIds == null || knowledgeIds.isEmpty()) {
            return RagRetrieveResult.empty();
        }
        String q = query == null ? "" : query.trim();
        if (q.isBlank()) {
            return RagRetrieveResult.empty();
        }
        RagRetrieveOptions opts = options == null ? RagRetrieveOptions.defaults() : options;
        int topK = Math.max(1, Math.min(AgentConsts.MAX_TOP_K, opts.topK()));
        int fetchK = opts.resolveFetchK();

        List<ScoredChunk> chunks = new ArrayList<>();
        for (Long kid : knowledgeIds) {
            if (kid == null) {
                continue;
            }
            AgentKnowledge kb = knowledgeMapper.selectById(kid);
            if (kb == null || kb.getStatus() == null || kb.getStatus() != SystemConsts.STATUS_ENABLE) {
                continue;
            }
            try {
                chunks.addAll(searchOne(kb, q, fetchK, opts.scoreThreshold(), opts));
            } catch (Exception e) {
                log.warn("知识库检索失败 knowledgeId={}: {}", kid, e.getMessage());
            }
        }
        chunks = applyRerankOrTrim(q, chunks, topK, opts);
        chunks = enrichDocTitles(chunks);
        String block = format(chunks, maxChars);
        List<RagRetrieveResult.Hit> hits = new ArrayList<>(chunks.size());
        for (ScoredChunk c : chunks) {
            hits.add(new RagRetrieveResult.Hit(
                    c.kbName, c.knowledgeId, c.docId, c.docTitle, c.chunkIndex, c.score,
                    c.content == null ? "" : c.content));
        }
        return new RagRetrieveResult(block, hits.size(), hits);
    }

    @Override
    public String retrieveOne(AgentKnowledge kb, String query, Integer topkOverride, Integer maxChars) {
        return retrieveOne(kb, query, topkOverride, maxChars, null);
    }

    @Override
    public String retrieveOne(AgentKnowledge kb, String query, Integer topkOverride, Integer maxChars,
                              Double scoreThreshold) {
        if (kb == null) {
            return "";
        }
        String q = query == null ? "" : query.trim();
        if (q.isBlank()) {
            return "（查询为空，跳过向量检索）";
        }
        try {
            int topK = topkOverride != null && topkOverride > 0 ? Math.min(AgentConsts.MAX_TOP_K, topkOverride)
                    : RagRetrieveOptions.DEFAULT_TOP_K;
            double threshold = resolveScoreThreshold(kb, scoreThreshold);
            List<ScoredChunk> chunks = searchOne(kb, q, topK, threshold, RagRetrieveOptions.defaults());
            // null / <=0：不截断，拼完整 TopK 分块
            String body = format(chunks, maxChars == null ? 0 : maxChars);
            if (body.isBlank()) {
                return "（知识库「" + kb.getName() + "」未命中相关分块）";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("## 知识库检索「").append(kb.getName()).append("」\n");
            sb.append("查询: ").append(q).append("\n\n");
            sb.append(body);
            return sb.toString().trim();
        } catch (Exception e) {
            log.warn("知识库检索失败 knowledgeId={}: {}", kb.getId(), e.getMessage());
            return "（知识库检索失败: " + e.getMessage() + "）";
        }
    }

    private static double resolveScoreThreshold(AgentKnowledge kb, Double override) {
        if (override != null && !override.isNaN()) {
            return Math.max(0d, Math.min(1d, override));
        }
        if (kb != null && kb.getScoreThreshold() != null) {
            return Math.max(0d, Math.min(1d, kb.getScoreThreshold().doubleValue()));
        }
        return RagRetrieveOptions.DEFAULT_SCORE_THRESHOLD;
    }

    private List<ScoredChunk> searchOne(AgentKnowledge kb, String query, int topK, double threshold,
                                        RagRetrieveOptions opts) {
        EmbeddingRef emb = resolveQueryEmbedding(kb, opts);
        if (emb == null) {
            return List.of();
        }
        float[] qv = embeddingClient.embedOne(emb.baseUrl(), emb.apiKey(), emb.model(), query);
        List<AgentVectorStore.Hit> hits = vectorStoreManager.store().search(kb.getId(), qv, topK, threshold);
        List<ScoredChunk> out = new ArrayList<>(hits.size());
        long knowledgeId = kb.getId() == null ? 0L : kb.getId();
        for (AgentVectorStore.Hit h : hits) {
            out.add(new ScoredChunk(kb.getName(), knowledgeId, h.docId(), "", h.chunkIndex(), h.content(), h.score()));
        }
        return out;
    }

    /** 批量补全文档标题，便于前端展示可点击来源。 */
    private List<ScoredChunk> enrichDocTitles(List<ScoredChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }
        Set<Long> docIds = new LinkedHashSet<>();
        for (ScoredChunk c : chunks) {
            if (c.docId > 0) {
                docIds.add(c.docId);
            }
        }
        Map<Long, AgentKnowledgeDoc> docs = new HashMap<>();
        if (!docIds.isEmpty()) {
            List<AgentKnowledgeDoc> rows = knowledgeDocMapper.selectByIds(docIds);
            if (rows != null) {
                for (AgentKnowledgeDoc d : rows) {
                    if (d != null && d.getId() != null) {
                        docs.put(d.getId(), d);
                    }
                }
            }
        }
        List<ScoredChunk> out = new ArrayList<>(chunks.size());
        for (ScoredChunk c : chunks) {
            AgentKnowledgeDoc d = docs.get(c.docId);
            String title = "";
            long knowledgeId = c.knowledgeId;
            if (d != null) {
                if (d.getTitle() != null && !d.getTitle().isBlank()) {
                    title = d.getTitle().trim();
                } else if (d.getFileName() != null && !d.getFileName().isBlank()) {
                    title = d.getFileName().trim();
                }
                if (d.getKnowledgeId() != null) {
                    knowledgeId = d.getKnowledgeId();
                }
            }
            if (title.isBlank()) {
                title = "文档 #" + c.docId;
            }
            out.add(new ScoredChunk(c.kbName, knowledgeId, c.docId, title, c.chunkIndex, c.content, c.score));
        }
        return out;
    }

    /** 优先智能体查询 Embedding，否则用知识库索引时的 Embedding（须与入库模型一致）。 */
    private EmbeddingRef resolveQueryEmbedding(AgentKnowledge kb, RagRetrieveOptions opts) {
        if (opts != null && opts.hasQueryEmbedding()) {
            AgentProvider provider = providerMapper.selectById(opts.embeddingProviderId());
            if (provider != null && provider.getStatus() != null && provider.getStatus() == SystemConsts.STATUS_ENABLE) {
                return new EmbeddingRef(provider.getBaseUrl(), provider.getApiKey(), opts.embeddingModel());
            }
        }
        if (kb.getEmbeddingProviderId() == null
                || kb.getEmbeddingModel() == null
                || kb.getEmbeddingModel().isBlank()) {
            return null;
        }
        AgentProvider provider = providerMapper.selectById(kb.getEmbeddingProviderId());
        if (provider == null || provider.getStatus() == null || provider.getStatus() != SystemConsts.STATUS_ENABLE) {
            return null;
        }
        return new EmbeddingRef(provider.getBaseUrl(), provider.getApiKey(), kb.getEmbeddingModel());
    }

    private List<ScoredChunk> applyRerankOrTrim(String query, List<ScoredChunk> chunks, int topK,
                                                RagRetrieveOptions opts) {
        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }
        chunks.sort(Comparator.comparingDouble(ScoredChunk::score).reversed());
        if (opts == null || !opts.hasRerank()) {
            return chunks.size() <= topK ? chunks : new ArrayList<>(chunks.subList(0, topK));
        }
        AgentProvider provider = providerMapper.selectById(opts.rerankProviderId());
        if (provider == null || provider.getStatus() == null || provider.getStatus() != SystemConsts.STATUS_ENABLE) {
            log.warn("Rerank 供应商不可用，回退向量排序");
            return chunks.size() <= topK ? chunks : new ArrayList<>(chunks.subList(0, topK));
        }
        try {
            List<String> docs = new ArrayList<>(chunks.size());
            for (ScoredChunk c : chunks) {
                docs.add(c.content == null ? "" : c.content);
            }
            List<Integer> order = rerankClient.rerank(
                    provider.getBaseUrl(), provider.getApiKey(), opts.rerankModel(),
                    query, docs, topK);
            List<ScoredChunk> reranked = new ArrayList<>(order.size());
            for (Integer idx : order) {
                if (idx != null && idx >= 0 && idx < chunks.size()) {
                    reranked.add(chunks.get(idx));
                }
            }
            return reranked.isEmpty()
                    ? (chunks.size() <= topK ? chunks : new ArrayList<>(chunks.subList(0, topK)))
                    : reranked;
        } catch (Exception e) {
            log.warn("Rerank 失败，回退向量排序: {}", e.getMessage());
            return chunks.size() <= topK ? chunks : new ArrayList<>(chunks.subList(0, topK));
        }
    }

    private static String format(List<ScoredChunk> chunks, int maxChars) {
        if (chunks == null || chunks.isEmpty()) {
            return "";
        }
        boolean unlimited = maxChars <= 0;
        int remain = unlimited ? 0 : Math.max(200, maxChars);
        StringBuilder sb = new StringBuilder();
        for (ScoredChunk c : chunks) {
            String docLabel = (c.docTitle == null || c.docTitle.isBlank()) ? ("doc-" + c.docId) : c.docTitle;
            String title = (c.kbName == null ? "知识" : c.kbName) + " / " + docLabel + " #" + c.chunkIndex
                    + " (score=" + String.format("%.3f", c.score) + ")";
            String body = c.content == null ? "" : c.content.trim();
            String piece = "### " + title + "\n" + body + "\n\n";
            if (unlimited) {
                sb.append(piece);
                continue;
            }
            if (remain <= 0) {
                break;
            }
            if (piece.length() > remain) {
                sb.append(piece, 0, remain);
                remain = 0;
            } else {
                sb.append(piece);
                remain -= piece.length();
            }
        }
        return sb.toString().trim();
    }

    private record EmbeddingRef(String baseUrl, String apiKey, String model) {
    }

    private record ScoredChunk(
            String kbName,
            long knowledgeId,
            long docId,
            String docTitle,
            int chunkIndex,
            String content,
            float score
    ) {
    }
}
