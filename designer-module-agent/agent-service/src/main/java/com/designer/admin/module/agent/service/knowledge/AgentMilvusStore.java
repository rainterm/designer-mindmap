package com.designer.admin.module.agent.service.knowledge;

import com.designer.admin.common.exception.BizException;
import com.designer.admin.module.agent.constant.AgentConsts;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.AddFieldReq;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.DropCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.collection.request.LoadCollectionReq;
import io.milvus.v2.service.vector.request.DeleteReq;
import io.milvus.v2.service.vector.request.QueryReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.UpsertReq;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.QueryResp;
import io.milvus.v2.service.vector.response.SearchResp;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Destroy;
import org.noear.solon.annotation.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Milvus 向量存储：每知识库一个 collection。
 *
 * @author 雨夜终端
 */
@Component
public class AgentMilvusStore {

    private static final Logger log = LoggerFactory.getLogger(AgentMilvusStore.class);
    private static final int CONTENT_MAX = 65535;
    private static final long CHUNK_ID_STRIDE = 1_000_000L;

    @Inject("${designer.agent.milvus.uri:http://localhost:19530}")
    private String uri;
    @Inject("${designer.agent.milvus.token:}")
    private String token;
    @Inject("${designer.agent.milvus.database:default}")
    private String database;

    private final Object lock = new Object();
    private volatile MilvusClientV2 client;

    public static String collectionName(long knowledgeId) {
        return "agent_kb_" + knowledgeId;
    }

    public static long chunkId(long docId, int chunkIndex) {
        if (chunkIndex < 0 || chunkIndex >= CHUNK_ID_STRIDE) {
            throw new BizException("分块序号超出范围: " + chunkIndex);
        }
        return docId * CHUNK_ID_STRIDE + chunkIndex;
    }

    public void ensureCollection(long knowledgeId, int dim) {
        if (dim <= 0) {
            throw new BizException("向量维度无效: " + dim);
        }
        String name = collectionName(knowledgeId);
        MilvusClientV2 c = client();
        Boolean exists = c.hasCollection(HasCollectionReq.builder().collectionName(name).build());
        if (Boolean.TRUE.equals(exists)) {
            c.loadCollection(LoadCollectionReq.builder().collectionName(name).build());
            return;
        }
        CreateCollectionReq.CollectionSchema schema = c.createSchema();
        schema.addField(AddFieldReq.builder()
                .fieldName("id")
                .dataType(DataType.Int64)
                .isPrimaryKey(true)
                .autoID(false)
                .build());
        schema.addField(AddFieldReq.builder()
                .fieldName("doc_id")
                .dataType(DataType.Int64)
                .build());
        schema.addField(AddFieldReq.builder()
                .fieldName("chunk_index")
                .dataType(DataType.Int32)
                .build());
        schema.addField(AddFieldReq.builder()
                .fieldName("content")
                .dataType(DataType.VarChar)
                .maxLength(CONTENT_MAX)
                .build());
        schema.addField(AddFieldReq.builder()
                .fieldName("vector")
                .dataType(DataType.FloatVector)
                .dimension(dim)
                .build());
        IndexParam indexParam = IndexParam.builder()
                .fieldName("vector")
                .indexType(IndexParam.IndexType.AUTOINDEX)
                .metricType(IndexParam.MetricType.COSINE)
                .build();
        c.createCollection(CreateCollectionReq.builder()
                .collectionName(name)
                .collectionSchema(schema)
                .indexParams(List.of(indexParam))
                .build());
        c.loadCollection(LoadCollectionReq.builder().collectionName(name).build());
        log.info("Created Milvus collection {} dim={}", name, dim);
    }

    public void dropCollection(long knowledgeId) {
        String name = collectionName(knowledgeId);
        MilvusClientV2 c = client();
        Boolean exists = c.hasCollection(HasCollectionReq.builder().collectionName(name).build());
        if (!Boolean.TRUE.equals(exists)) {
            return;
        }
        c.dropCollection(DropCollectionReq.builder().collectionName(name).build());
        log.info("Dropped Milvus collection {}", name);
    }

    public void deleteByDocId(long knowledgeId, long docId) {
        String name = collectionName(knowledgeId);
        MilvusClientV2 c = client();
        Boolean exists = c.hasCollection(HasCollectionReq.builder().collectionName(name).build());
        if (!Boolean.TRUE.equals(exists)) {
            return;
        }
        c.delete(DeleteReq.builder()
                .collectionName(name)
                .filter("doc_id == " + docId)
                .build());
    }

    public List<ChunkRow> listByDocId(long knowledgeId, long docId) {
        String name = collectionName(knowledgeId);
        MilvusClientV2 c = client();
        Boolean exists = c.hasCollection(HasCollectionReq.builder().collectionName(name).build());
        if (!Boolean.TRUE.equals(exists)) {
            return List.of();
        }
        c.loadCollection(LoadCollectionReq.builder().collectionName(name).build());
        QueryResp resp = c.query(QueryReq.builder()
                .collectionName(name)
                .filter("doc_id == " + docId)
                .outputFields(List.of("id", "doc_id", "chunk_index", "content"))
                .limit(10_000L)
                .build());
        List<QueryResp.QueryResult> results = resp == null ? null : resp.getQueryResults();
        if (results == null || results.isEmpty()) {
            return List.of();
        }
        List<ChunkRow> rows = new ArrayList<>(results.size());
        for (QueryResp.QueryResult r : results) {
            Map<String, Object> entity = r.getEntity();
            if (entity == null) {
                continue;
            }
            rows.add(new ChunkRow(
                    toLong(entity.get("id")),
                    toLong(entity.get("doc_id")),
                    (int) toLong(entity.get("chunk_index")),
                    String.valueOf(entity.getOrDefault("content", ""))));
        }
        rows.sort(Comparator.comparingInt(ChunkRow::chunkIndex));
        return rows;
    }

    public ChunkRow getChunk(long knowledgeId, long docId, int chunkIndex) {
        String name = collectionName(knowledgeId);
        MilvusClientV2 c = client();
        Boolean exists = c.hasCollection(HasCollectionReq.builder().collectionName(name).build());
        if (!Boolean.TRUE.equals(exists)) {
            return null;
        }
        c.loadCollection(LoadCollectionReq.builder().collectionName(name).build());
        long id = chunkId(docId, chunkIndex);
        QueryResp resp = c.query(QueryReq.builder()
                .collectionName(name)
                .filter("id == " + id)
                .outputFields(List.of("id", "doc_id", "chunk_index", "content"))
                .limit(1L)
                .build());
        List<QueryResp.QueryResult> results = resp == null ? null : resp.getQueryResults();
        if (results == null || results.isEmpty()) {
            return null;
        }
        Map<String, Object> entity = results.get(0).getEntity();
        if (entity == null) {
            return null;
        }
        return new ChunkRow(
                toLong(entity.get("id")),
                toLong(entity.get("doc_id")),
                (int) toLong(entity.get("chunk_index")),
                String.valueOf(entity.getOrDefault("content", "")));
    }

    /** 更新单条分块（同 id upsert，保留序号）。 */
    public void upsertChunk(long knowledgeId, long docId, int chunkIndex, String content, float[] vector) {
        if (vector == null || vector.length == 0) {
            throw new BizException("向量为空");
        }
        ensureCollection(knowledgeId, vector.length);
        String text = content == null ? "" : content;
        if (text.length() > CONTENT_MAX) {
            text = text.substring(0, CONTENT_MAX);
        }
        JsonObject row = new JsonObject();
        row.addProperty("id", chunkId(docId, chunkIndex));
        row.addProperty("doc_id", docId);
        row.addProperty("chunk_index", chunkIndex);
        row.addProperty("content", text);
        JsonArray arr = new JsonArray();
        for (float f : vector) {
            arr.add(f);
        }
        row.add("vector", arr);
        client().upsert(UpsertReq.builder()
                .collectionName(collectionName(knowledgeId))
                .data(List.of(row))
                .build());
    }

    public void upsertChunks(long knowledgeId, long docId, List<String> contents, List<float[]> vectors) {
        if (contents == null || contents.isEmpty()) {
            deleteByDocId(knowledgeId, docId);
            return;
        }
        if (vectors == null || vectors.size() != contents.size()) {
            throw new BizException("分块与向量数量不一致");
        }
        int dim = vectors.get(0).length;
        ensureCollection(knowledgeId, dim);
        deleteByDocId(knowledgeId, docId);

        List<JsonObject> rows = new ArrayList<>(contents.size());
        for (int i = 0; i < contents.size(); i++) {
            String content = contents.get(i);
            if (content != null && content.length() > CONTENT_MAX) {
                content = content.substring(0, CONTENT_MAX);
            }
            float[] vec = vectors.get(i);
            if (vec.length != dim) {
                throw new BizException("向量维度不一致");
            }
            JsonObject row = new JsonObject();
            row.addProperty("id", chunkId(docId, i));
            row.addProperty("doc_id", docId);
            row.addProperty("chunk_index", i);
            row.addProperty("content", content == null ? "" : content);
            JsonArray arr = new JsonArray();
            for (float f : vec) {
                arr.add(f);
            }
            row.add("vector", arr);
            rows.add(row);
        }
        client().upsert(UpsertReq.builder()
                .collectionName(collectionName(knowledgeId))
                .data(rows)
                .build());
    }

    public List<Hit> search(long knowledgeId, float[] queryVector, int topK, double scoreThreshold) {
        String name = collectionName(knowledgeId);
        MilvusClientV2 c = client();
        Boolean exists = c.hasCollection(HasCollectionReq.builder().collectionName(name).build());
        if (!Boolean.TRUE.equals(exists)) {
            return List.of();
        }
        c.loadCollection(LoadCollectionReq.builder().collectionName(name).build());
        int k = Math.max(1, Math.min(topK, AgentConsts.MAX_TOP_K));
        SearchResp resp = c.search(SearchReq.builder()
                .collectionName(name)
                .data(List.of(new FloatVec(queryVector)))
                .annsField("vector")
                .topK(k)
                .metricType(IndexParam.MetricType.COSINE)
                .outputFields(List.of("doc_id", "chunk_index", "content"))
                .build());
        List<List<SearchResp.SearchResult>> groups = resp.getSearchResults();
        if (groups == null || groups.isEmpty() || groups.get(0) == null) {
            return List.of();
        }
        List<Hit> hits = new ArrayList<>();
        for (SearchResp.SearchResult r : groups.get(0)) {
            float score = r.getScore() == null ? 0f : r.getScore();
            if (score < scoreThreshold) {
                continue;
            }
            Map<String, Object> entity = r.getEntity();
            String content = entity == null ? "" : String.valueOf(entity.getOrDefault("content", ""));
            long docId = toLong(entity == null ? null : entity.get("doc_id"));
            int chunkIndex = (int) toLong(entity == null ? null : entity.get("chunk_index"));
            hits.add(new Hit(docId, chunkIndex, content, score));
        }
        return hits;
    }

    private MilvusClientV2 client() {
        MilvusClientV2 local = client;
        if (local != null) {
            return local;
        }
        synchronized (lock) {
            if (client != null) {
                return client;
            }
            try {
                ConnectConfig.ConnectConfigBuilder<?, ?> builder = ConnectConfig.builder()
                        .uri(uri == null || uri.isBlank() ? "http://localhost:19530" : uri.trim());
                if (token != null && !token.isBlank()) {
                    builder.token(token.trim());
                }
                if (database != null && !database.isBlank() && !"default".equals(database.trim())) {
                    builder.dbName(database.trim());
                }
                client = new MilvusClientV2(builder.build());
                return client;
            } catch (Exception e) {
                throw new BizException("连接 Milvus 失败: " + e.getMessage(), e);
            }
        }
    }

    @Destroy
    public void destroy() {
        synchronized (lock) {
            if (client != null) {
                try {
                    client.close();
                } catch (Exception e) {
                    log.warn("关闭 Milvus 客户端失败", e);
                }
                client = null;
            }
        }
    }

    private static long toLong(Object v) {
        if (v == null) {
            return 0L;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (Exception e) {
            return 0L;
        }
    }

    public record Hit(long docId, int chunkIndex, String content, float score) {
    }

    public record ChunkRow(long id, long docId, int chunkIndex, String content) {
    }
}
