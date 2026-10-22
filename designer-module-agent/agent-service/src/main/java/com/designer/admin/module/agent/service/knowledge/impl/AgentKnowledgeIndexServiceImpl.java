package com.designer.admin.module.agent.service.knowledge.impl;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Destroy;
import org.noear.solon.annotation.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.designer.admin.common.constant.SystemConsts;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.module.agent.constant.knowledge.KnowledgeDocStatus;
import com.designer.admin.module.agent.mapper.knowledge.AgentKnowledgeDocMapper;
import com.designer.admin.module.agent.mapper.knowledge.AgentKnowledgeMapper;
import com.designer.admin.module.agent.mapper.provider.AgentProviderMapper;
import com.designer.admin.module.agent.model.entity.knowledge.AgentKnowledge;
import com.designer.admin.module.agent.model.entity.knowledge.AgentKnowledgeDoc;
import com.designer.admin.module.agent.model.entity.provider.AgentProvider;
import com.designer.admin.module.agent.service.knowledge.AgentEmbeddingClient;
import com.designer.admin.module.agent.service.knowledge.AgentKnowledgeIndexService;
import com.designer.admin.module.agent.service.knowledge.AgentKnowledgeTextExtractor;
import com.designer.admin.module.agent.service.knowledge.AgentMilvusStore;
import com.designer.admin.module.agent.constant.AgentConsts;
import com.designer.admin.module.agent.service.knowledge.AgentTextChunker;

/**
 * 文档分块 → Embedding → 写入 Milvus。
 *
 * @author 雨夜终端
 */
@Component
public class AgentKnowledgeIndexServiceImpl implements AgentKnowledgeIndexService {

    private static final Logger log = LoggerFactory.getLogger(AgentKnowledgeIndexServiceImpl.class);
    private static final ThreadLocal<Consumer<AgentKnowledgeDoc>> PROGRESS = new ThreadLocal<>();

    @Db
    private AgentKnowledgeMapper knowledgeMapper;
    @Db
    private AgentKnowledgeDocMapper docMapper;
    @Db
    private AgentProviderMapper providerMapper;
    @Inject
    private AgentEmbeddingClient embeddingClient;
    @Inject
    private AgentMilvusStore milvusStore;
    @Inject
    private AgentKnowledgeTextExtractor textExtractor;

    /** 知识库异步索引线程池：显式队列与拒绝策略，禁止用 Executors 工厂（见开发规范 4.13） */
    private final ExecutorService executor = new ThreadPoolExecutor(
            2,
            2,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(200),
            new ThreadFactory() {
                private final AtomicInteger seq = new AtomicInteger();

                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "kb-index-" + seq.incrementAndGet());
                    t.setDaemon(true);
                    return t;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy());

    @Override
    public void indexDocAsync(Long docId) {
        if (docId == null) {
            return;
        }
        AgentKnowledgeDoc doc = docMapper.selectById(docId);
        if (doc == null) {
            return;
        }
        markIndexing(doc, 0, "排队中…");
        executor.submit(() -> {
            try {
                indexDoc(docId);
            } catch (Exception e) {
                log.warn("异步索引失败 docId={}: {}", docId, e.getMessage());
            }
        });
    }

    @Override
    public AgentKnowledgeDoc indexDoc(Long docId) {
        return indexDoc(docId, null);
    }

    @Override
    public AgentKnowledgeDoc indexDoc(Long docId, Consumer<AgentKnowledgeDoc> onProgress) {
        AgentKnowledgeDoc doc = docMapper.selectById(docId);
        if (doc == null) {
            throw new BizException("文档不存在");
        }
        PROGRESS.set(onProgress);
        try {
            return indexDoc(doc);
        } finally {
            PROGRESS.remove();
        }
    }

    @Override
    public AgentKnowledgeDoc indexDoc(AgentKnowledgeDoc doc) {
        AgentKnowledge kb = knowledgeMapper.selectById(doc.getKnowledgeId());
        if (kb == null) {
            throw new BizException("知识库不存在");
        }
        markIndexing(doc, 5, "准备中…");

        try {
            String content = doc.getContent() == null ? "" : doc.getContent().trim();
            if (content.isBlank() && doc.getUrl() != null && !doc.getUrl().isBlank()) {
                markIndexing(doc, 8, "抽取正文…");
                content = textExtractor.extract(doc.getUrl(), doc.getFileName(), doc.getFileType());
                if (content != null && !content.isBlank()) {
                    doc.setContent(content);
                    doc.setCharCount(content.length());
                    docMapper.updateById(doc);
                }
            }
            content = content == null ? "" : content.trim();
            int charCount = content.length();
            doc.setCharCount(charCount);

            if (content.isBlank()) {
                milvusStore.deleteByDocId(kb.getId(), doc.getId());
                doc.setChunkCount(0);
                doc.setIndexProgress(0);
                doc.setStatus(KnowledgeDocStatus.PENDING.getCode());
                doc.setErrorMsg("正文为空，无法建索引；请补充文本后重索引");
                docMapper.updateById(doc);
                emitProgress(doc);
                return doc;
            }

            markIndexing(doc, 10, "分块中…");
            AgentProvider provider = requireEmbeddingProvider(kb);
            int chunkSize = kb.getChunkSize() == null ? AgentConsts.DEFAULT_CHUNK_SIZE : kb.getChunkSize();
            int overlap = kb.getChunkOverlap() == null ? AgentConsts.DEFAULT_CHUNK_OVERLAP : kb.getChunkOverlap();
            String strategy = AgentTextChunker.normalizeStrategy(kb.getChunkStrategy());
            List<String> chunks = AgentTextChunker.chunk(content, strategy, chunkSize, overlap);
            if (chunks.isEmpty()) {
                milvusStore.deleteByDocId(kb.getId(), doc.getId());
                doc.setChunkCount(0);
                doc.setIndexProgress(0);
                doc.setStatus(KnowledgeDocStatus.PENDING.getCode());
                doc.setErrorMsg("分块结果为空");
                docMapper.updateById(doc);
                emitProgress(doc);
                return doc;
            }

            doc.setChunkCount(chunks.size());
            markIndexing(doc, 15, "向量化 0/" + chunks.size());

            List<float[]> vectors = embeddingClient.embed(
                    provider.getBaseUrl(), provider.getApiKey(), kb.getEmbeddingModel(), chunks,
                    (done, total) -> {
                        int pct = 15 + (int) Math.round(done * 70.0 / Math.max(1, total));
                        markIndexing(doc, Math.min(85, pct), "向量化 " + done + "/" + total);
                    });
            int dim = vectors.get(0).length;
            Integer lockedDim = kb.getEmbeddingDim();
            if (lockedDim != null && lockedDim > 0 && lockedDim != dim) {
                milvusStore.dropCollection(kb.getId());
            }

            markIndexing(doc, 90, "写入向量库…");
            milvusStore.upsertChunks(kb.getId(), doc.getId(), chunks, vectors);

            if (lockedDim == null || lockedDim <= 0 || lockedDim != dim) {
                kb.setEmbeddingDim(dim);
                knowledgeMapper.updateById(kb);
            }

            doc.setChunkCount(chunks.size());
            doc.setIndexProgress(100);
            doc.setStatus(KnowledgeDocStatus.READY.getCode());
            doc.setErrorMsg("");
            docMapper.updateById(doc);
            emitProgress(doc);
            return doc;
        } catch (Exception e) {
            log.warn("知识库文档索引失败 docId={}: {}", doc.getId(), e.getMessage());
            doc.setStatus(KnowledgeDocStatus.FAILED.getCode());
            doc.setIndexProgress(0);
            doc.setErrorMsg(abbreviate(e.getMessage(), 480));
            docMapper.updateById(doc);
            emitProgress(doc);
            if (e instanceof BizException be) {
                throw be;
            }
            throw new BizException("索引失败: " + e.getMessage());
        }
    }

    @Override
    public List<AgentMilvusStore.ChunkRow> listChunks(Long docId) {
        AgentKnowledgeDoc doc = docMapper.selectById(docId);
        if (doc == null) {
            throw new BizException("文档不存在");
        }
        return milvusStore.listByDocId(doc.getKnowledgeId(), doc.getId());
    }

    @Override
    public AgentMilvusStore.ChunkRow updateChunk(Long docId, int chunkIndex, String content) {
        if (content == null || content.isBlank()) {
            throw new BizException("分块内容不能为空");
        }
        AgentKnowledgeDoc doc = docMapper.selectById(docId);
        if (doc == null) {
            throw new BizException("文档不存在");
        }
        AgentKnowledge kb = knowledgeMapper.selectById(doc.getKnowledgeId());
        if (kb == null) {
            throw new BizException("知识库不存在");
        }
        AgentMilvusStore.ChunkRow existing = milvusStore.getChunk(kb.getId(), doc.getId(), chunkIndex);
        if (existing == null) {
            throw new BizException("分块不存在，请先完成索引");
        }
        AgentProvider provider = requireEmbeddingProvider(kb);
        String text = content.trim();
        float[] vector = embeddingClient.embedOne(
                provider.getBaseUrl(), provider.getApiKey(), kb.getEmbeddingModel(), text);
        milvusStore.upsertChunk(kb.getId(), doc.getId(), chunkIndex, text, vector);
        return new AgentMilvusStore.ChunkRow(
                AgentMilvusStore.chunkId(doc.getId(), chunkIndex),
                doc.getId(),
                chunkIndex,
                text);
    }

    @Override
    public void deleteDocVectors(long knowledgeId, long docId) {
        try {
            milvusStore.deleteByDocId(knowledgeId, docId);
        } catch (Exception e) {
            log.warn("删除文档向量失败 knowledgeId={} docId={}: {}", knowledgeId, docId, e.getMessage());
        }
    }

    @Override
    public void dropKnowledgeVectors(long knowledgeId) {
        try {
            milvusStore.dropCollection(knowledgeId);
        } catch (Exception e) {
            log.warn("删除知识库向量失败 knowledgeId={}: {}", knowledgeId, e.getMessage());
        }
    }

    @Override
    public void reindexAllReady(Long knowledgeId) {
        List<AgentKnowledgeDoc> docs = readyDocs(knowledgeId);
        milvusStore.dropCollection(knowledgeId);
        AgentKnowledge kb = knowledgeMapper.selectById(knowledgeId);
        if (kb != null) {
            kb.setEmbeddingDim(0);
            knowledgeMapper.updateById(kb);
        }
        for (AgentKnowledgeDoc doc : docs) {
            indexDocAsync(doc.getId());
        }
    }

    @Override
    public void rechunkAllReady(Long knowledgeId) {
        for (AgentKnowledgeDoc doc : readyDocs(knowledgeId)) {
            indexDocAsync(doc.getId());
        }
    }

    private List<AgentKnowledgeDoc> readyDocs(Long knowledgeId) {
        return docMapper.selectList(new LambdaQueryWrapper<AgentKnowledgeDoc>()
                .eq(AgentKnowledgeDoc::getKnowledgeId, knowledgeId)
                .eq(AgentKnowledgeDoc::getStatus, KnowledgeDocStatus.READY.getCode()));
    }

    private void markIndexing(AgentKnowledgeDoc doc, int progress, String stage) {
        doc.setStatus(KnowledgeDocStatus.INDEXING.getCode());
        doc.setIndexProgress(Math.max(0, Math.min(100, progress)));
        doc.setErrorMsg(stage == null ? "" : abbreviate(stage, 100));
        docMapper.updateById(doc);
        emitProgress(doc);
    }

    private void emitProgress(AgentKnowledgeDoc doc) {
        Consumer<AgentKnowledgeDoc> listener = PROGRESS.get();
        if (listener != null) {
            try {
                listener.accept(doc);
            } catch (Exception e) {
                log.debug("索引进度回调失败 docId={}: {}", doc.getId(), e.getMessage());
            }
        }
    }

    private AgentProvider requireEmbeddingProvider(AgentKnowledge kb) {
        if (kb.getEmbeddingProviderId() == null) {
            throw new BizException("请先为知识库配置 Embedding 供应商");
        }
        if (kb.getEmbeddingModel() == null || kb.getEmbeddingModel().isBlank()) {
            throw new BizException("请先为知识库配置 Embedding 模型");
        }
        AgentProvider provider = providerMapper.selectById(kb.getEmbeddingProviderId());
        if (provider == null || provider.getStatus() == null || provider.getStatus() != SystemConsts.STATUS_ENABLE) {
            throw new BizException("Embedding 供应商不可用");
        }
        if (provider.getBaseUrl() == null || provider.getBaseUrl().isBlank()) {
            throw new BizException("Embedding 供应商 Base URL 为空");
        }
        return provider;
    }

    private static String abbreviate(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max) + "…";
    }

    @Destroy
    public void destroy() {
        executor.shutdownNow();
    }
}
