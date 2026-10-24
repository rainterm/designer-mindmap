package com.designer.admin.module.agent.service.knowledge.vector;

import com.designer.admin.common.exception.BizException;

import java.util.List;

/**
 * 知识库向量存储 SPI：按知识库隔离索引，支持分块写入与余弦相似度检索。
 *
 * <p>实现通过 {@code designer.agent.vector.type} 选择（见 {@link AgentVectorStoreManager}）。
 * 新增实现：标注 {@code @Component}、{@link #type()} 返回唯一标识即可。</p>
 *
 * @author 雨夜终端
 */
public interface AgentVectorStore {

    /** 与配置 {@code designer.agent.vector.type} 对应的实现标识。 */
    String type();

    /**
     * 被选中启用时调用（校验依赖是否可用）；默认无操作。
     */
    default void onSelected() {
    }

    /** 每知识库一个索引名（Milvus collection / Redis RediSearch index）。 */
    static String indexName(long knowledgeId) {
        return "agent_kb_" + knowledgeId;
    }

    /** 分块主键：{@code docId * 1_000_000 + chunkIndex}。 */
    static long chunkId(long docId, int chunkIndex) {
        if (chunkIndex < 0 || chunkIndex >= 1_000_000L) {
            throw new BizException("分块序号超出范围: " + chunkIndex);
        }
        return docId * 1_000_000L + chunkIndex;
    }

    void ensureCollection(long knowledgeId, int dim);

    void dropCollection(long knowledgeId);

    void deleteByDocId(long knowledgeId, long docId);

    List<ChunkRow> listByDocId(long knowledgeId, long docId);

    ChunkRow getChunk(long knowledgeId, long docId, int chunkIndex);

    /** 更新单条分块（同 id upsert，保留序号）。 */
    void upsertChunk(long knowledgeId, long docId, int chunkIndex, String content, float[] vector);

    void upsertChunks(long knowledgeId, long docId, List<String> contents, List<float[]> vectors);

    /**
     * 余弦相似度检索；返回分数为相似度（越大越相关），低于阈值的结果已过滤。
     */
    List<Hit> search(long knowledgeId, float[] queryVector, int topK, double scoreThreshold);

    record Hit(long docId, int chunkIndex, String content, float score) {
    }

    record ChunkRow(long id, long docId, int chunkIndex, String content) {
    }
}
