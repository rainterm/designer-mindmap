package com.designer.admin.module.agent.service.knowledge;

import com.designer.admin.module.agent.model.entity.knowledge.AgentKnowledgeDoc;
import com.designer.admin.module.agent.service.knowledge.vector.AgentVectorStore;
import java.util.List;
import java.util.function.Consumer;

/**
 * 知识库文档索引
 *
 * @author 雨夜终端
 */
public interface AgentKnowledgeIndexService {

    /**
     * 后台异步索引（无 SSE 监听时使用，如更换 Embedding 后批量重建）。
     *
     * @param docId 文档 ID
     */
    void indexDocAsync(Long docId);

    /**
     * 同步索引文档。
     *
     * @param docId 文档 ID
     * @return 索引完成后的文档
     */
    AgentKnowledgeDoc indexDoc(Long docId);

    /**
     * 同步索引文档，并通过回调推送进度（供 SSE 使用）。
     *
     * @param docId      文档 ID
     * @param onProgress 进度回调，可空
     * @return 索引完成后的文档
     */
    AgentKnowledgeDoc indexDoc(Long docId, Consumer<AgentKnowledgeDoc> onProgress);

    /**
     * 同步索引已加载的文档实体。
     *
     * @param doc 文档实体
     * @return 索引完成后的文档
     */
    AgentKnowledgeDoc indexDoc(AgentKnowledgeDoc doc);

    /**
     * 列出文档在向量库中的全部分块。
     *
     * @param docId 文档 ID
     * @return 分块行列表
     */
    List<AgentVectorStore.ChunkRow> listChunks(Long docId);

    /**
     * 更新指定分块文本并重写向量。
     *
     * @param docId      文档 ID
     * @param chunkIndex 分块下标
     * @param content    新文本内容
     * @return 更新后的分块行
     */
    AgentVectorStore.ChunkRow updateChunk(Long docId, int chunkIndex, String content);

    /**
     * 删除指定文档在向量库中的全部向量。
     *
     * @param knowledgeId 知识库 ID
     * @param docId       文档 ID
     */
    void deleteDocVectors(long knowledgeId, long docId);

    /**
     * 删除知识库对应 collection / 全部向量。
     *
     * @param knowledgeId 知识库 ID
     */
    void dropKnowledgeVectors(long knowledgeId);

    /**
     * Embedding 配置变更导致维度变化时，清空 collection 并重索引 ready 文档。
     *
     * @param knowledgeId 知识库 ID
     */
    void reindexAllReady(Long knowledgeId);

    /**
     * 分块策略/大小变更：保留 collection，按文档重切重写。
     *
     * @param knowledgeId 知识库 ID
     */
    void rechunkAllReady(Long knowledgeId);

}
