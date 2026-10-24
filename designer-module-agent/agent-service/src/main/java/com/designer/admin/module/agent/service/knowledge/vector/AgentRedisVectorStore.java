package com.designer.admin.module.agent.service.knowledge.vector;

import com.designer.admin.common.exception.BizException;
import com.designer.admin.module.agent.constant.AgentConsts;
import org.noear.redisx.RedisClient;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;
import redis.clients.jedis.search.Document;
import redis.clients.jedis.search.IndexDefinition;
import redis.clients.jedis.search.IndexOptions;
import redis.clients.jedis.search.Query;
import redis.clients.jedis.search.Schema;
import redis.clients.jedis.search.SearchResult;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Redis（RediSearch VECTOR）向量存储：每知识库一个 FT 索引。
 *
 * <p>需要 Redis Stack 或已加载 search 模块的 Redis；复用业务 {@link RedisClient}。</p>
 *
 * @author 雨夜终端
 */
@Component
public class AgentRedisVectorStore implements AgentVectorStore {

    private static final Logger log = LoggerFactory.getLogger(AgentRedisVectorStore.class);
    private static final int CONTENT_MAX = 65535;
    private static final String TYPE = "redis";

    @Inject
    private RedisClient redisClient;
    @Inject("${designer.agent.vector.redis.key-prefix:agent:kb:}")
    private String keyPrefix;

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public void onSelected() {
        try {
            redisClient.open(s -> s.jedis().ftList());
        } catch (Exception e) {
            throw new IllegalStateException(
                    "designer.agent.vector.type=redis 需要 Redis Stack（或已加载 RediSearch/search 模块）: "
                            + e.getMessage(), e);
        }
        log.info("Redis 向量存储就绪，key-prefix={}", prefix());
    }

    @Override
    public void ensureCollection(long knowledgeId, int dim) {
        if (dim <= 0) {
            throw new BizException("向量维度无效: " + dim);
        }
        String index = AgentVectorStore.indexName(knowledgeId);
        redisClient.open(s -> {
            UnifiedJedis jedis = s.jedis();
            if (indexExists(jedis, index)) {
                return;
            }
            Map<String, Object> vectorAttrs = new HashMap<>();
            vectorAttrs.put("TYPE", "FLOAT32");
            vectorAttrs.put("DIM", dim);
            vectorAttrs.put("DISTANCE_METRIC", "COSINE");
            Schema schema = new Schema()
                    .addNumericField("id")
                    .addNumericField("doc_id")
                    .addNumericField("chunk_index")
                    .addTextField("content", 1.0)
                    .addHNSWVectorField("vector", vectorAttrs);
            IndexDefinition definition = new IndexDefinition()
                    .setPrefixes(new String[]{chunkKeyPrefix(knowledgeId)});
            try {
                jedis.ftCreate(index, IndexOptions.defaultOptions().setDefinition(definition), schema);
                log.info("Created Redis vector index {} dim={}", index, dim);
            } catch (JedisDataException e) {
                if (isIndexAlreadyExists(e)) {
                    return;
                }
                throw new BizException("创建 Redis 向量索引失败: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public void dropCollection(long knowledgeId) {
        String index = AgentVectorStore.indexName(knowledgeId);
        redisClient.open(s -> {
            UnifiedJedis jedis = s.jedis();
            if (indexExists(jedis, index)) {
                try {
                    jedis.ftDropIndexDD(index);
                } catch (JedisDataException e) {
                    log.warn("删除 Redis 向量索引失败 index={}: {}", index, e.getMessage());
                }
            }
            // 分块 Hash 由 DD 删除；清理 doc→chunk 辅助集合
            deleteByPattern(jedis, docSetKeyPrefix(knowledgeId) + "*");
        });
        log.info("Dropped Redis vector index {}", index);
    }

    @Override
    public void deleteByDocId(long knowledgeId, long docId) {
        String docSet = docSetKey(knowledgeId, docId);
        redisClient.open(s -> {
            UnifiedJedis jedis = s.jedis();
            Set<String> chunkKeys = jedis.smembers(docSet);
            if (chunkKeys != null && !chunkKeys.isEmpty()) {
                jedis.del(chunkKeys.toArray(String[]::new));
            }
            jedis.del(docSet);
        });
    }

    @Override
    public List<ChunkRow> listByDocId(long knowledgeId, long docId) {
        String docSet = docSetKey(knowledgeId, docId);
        return redisClient.openAndGet(s -> {
            UnifiedJedis jedis = s.jedis();
            Set<String> chunkKeys = jedis.smembers(docSet);
            if (chunkKeys == null || chunkKeys.isEmpty()) {
                return List.of();
            }
            List<ChunkRow> rows = new ArrayList<>(chunkKeys.size());
            for (String key : chunkKeys) {
                ChunkRow row = readChunkMeta(jedis, key);
                if (row != null) {
                    rows.add(row);
                }
            }
            rows.sort(Comparator.comparingInt(ChunkRow::chunkIndex));
            return rows;
        });
    }

    @Override
    public ChunkRow getChunk(long knowledgeId, long docId, int chunkIndex) {
        String key = chunkKey(knowledgeId, AgentVectorStore.chunkId(docId, chunkIndex));
        return redisClient.openAndGet(s -> readChunkMeta(s.jedis(), key));
    }

    @Override
    public void upsertChunk(long knowledgeId, long docId, int chunkIndex, String content, float[] vector) {
        if (vector == null || vector.length == 0) {
            throw new BizException("向量为空");
        }
        ensureCollection(knowledgeId, vector.length);
        writeChunk(knowledgeId, docId, chunkIndex, content, vector);
    }

    @Override
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
        for (int i = 0; i < contents.size(); i++) {
            float[] vec = vectors.get(i);
            if (vec.length != dim) {
                throw new BizException("向量维度不一致");
            }
            writeChunk(knowledgeId, docId, i, contents.get(i), vec);
        }
    }

    @Override
    public List<Hit> search(long knowledgeId, float[] queryVector, int topK, double scoreThreshold) {
        String index = AgentVectorStore.indexName(knowledgeId);
        int k = Math.max(1, Math.min(topK, AgentConsts.MAX_TOP_K));
        return redisClient.openAndGet(s -> {
            UnifiedJedis jedis = s.jedis();
            if (!indexExists(jedis, index)) {
                return List.of();
            }
            byte[] blob = floatsToBytes(queryVector);
            Query query = new Query("*=>[KNN $K @vector $BLOB AS distance]")
                    .returnFields("doc_id", "chunk_index", "content", "distance")
                    .addParam("K", k)
                    .addParam("BLOB", blob)
                    .setSortBy("distance", true)
                    .limit(0, k)
                    .dialect(2);
            SearchResult result;
            try {
                result = jedis.ftSearch(index, query);
            } catch (JedisDataException e) {
                throw new BizException("Redis 向量检索失败: " + e.getMessage(), e);
            }
            List<Document> docs = result == null ? null : result.getDocuments();
            if (docs == null || docs.isEmpty()) {
                return List.of();
            }
            List<Hit> hits = new ArrayList<>(docs.size());
            for (Document doc : docs) {
                float distance = parseFloat(doc.get("distance"));
                // Redis COSINE 返回距离 = 1 - 相似度；与 Milvus 对齐为相似度
                float score = 1f - distance;
                if (score < scoreThreshold) {
                    continue;
                }
                hits.add(new Hit(
                        parseLong(doc.get("doc_id")),
                        (int) parseLong(doc.get("chunk_index")),
                        doc.getString("content") == null ? "" : doc.getString("content"),
                        score));
            }
            return hits;
        });
    }

    /** 只读元数据字段，避开 binary 的 vector。 */
    private static ChunkRow readChunkMeta(UnifiedJedis jedis, String key) {
        List<String> vals = jedis.hmget(key, "id", "doc_id", "chunk_index", "content");
        if (vals == null || vals.isEmpty() || vals.get(0) == null) {
            return null;
        }
        return new ChunkRow(
                parseLong(vals.get(0)),
                parseLong(vals.size() > 1 ? vals.get(1) : null),
                (int) parseLong(vals.size() > 2 ? vals.get(2) : null),
                vals.size() > 3 && vals.get(3) != null ? vals.get(3) : "");
    }

    private void writeChunk(long knowledgeId, long docId, int chunkIndex, String content, float[] vector) {
        long id = AgentVectorStore.chunkId(docId, chunkIndex);
        String text = content == null ? "" : content;
        if (text.length() > CONTENT_MAX) {
            text = text.substring(0, CONTENT_MAX);
        }
        String key = chunkKey(knowledgeId, id);
        String docSet = docSetKey(knowledgeId, docId);
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        Map<byte[], byte[]> fields = new HashMap<>();
        fields.put(bytes("id"), bytes(String.valueOf(id)));
        fields.put(bytes("doc_id"), bytes(String.valueOf(docId)));
        fields.put(bytes("chunk_index"), bytes(String.valueOf(chunkIndex)));
        fields.put(bytes("content"), bytes(text));
        fields.put(bytes("vector"), floatsToBytes(vector));
        redisClient.open(s -> {
            UnifiedJedis jedis = s.jedis();
            jedis.hset(keyBytes, fields);
            jedis.sadd(docSet, key);
        });
    }

    private String prefix() {
        String p = keyPrefix == null || keyPrefix.isBlank() ? "agent:kb:" : keyPrefix.trim();
        return p.endsWith(":") ? p : p + ":";
    }

    private String chunkKeyPrefix(long knowledgeId) {
        return prefix() + knowledgeId + ":chunk:";
    }

    private String chunkKey(long knowledgeId, long chunkId) {
        return chunkKeyPrefix(knowledgeId) + chunkId;
    }

    private String docSetKeyPrefix(long knowledgeId) {
        return prefix() + knowledgeId + ":doc:";
    }

    private String docSetKey(long knowledgeId, long docId) {
        return docSetKeyPrefix(knowledgeId) + docId;
    }

    private static void deleteByPattern(UnifiedJedis jedis, String pattern) {
        String cursor = ScanParams.SCAN_POINTER_START;
        ScanParams params = new ScanParams().match(pattern).count(200);
        do {
            ScanResult<String> scan = jedis.scan(cursor, params);
            List<String> keys = scan.getResult();
            if (keys != null && !keys.isEmpty()) {
                jedis.del(keys.toArray(String[]::new));
            }
            cursor = scan.getCursor();
        } while (!ScanParams.SCAN_POINTER_START.equals(cursor));
    }

    private static boolean indexExists(UnifiedJedis jedis, String index) {
        try {
            jedis.ftInfo(index);
            return true;
        } catch (JedisDataException e) {
            return false;
        }
    }

    private static boolean isIndexAlreadyExists(JedisDataException e) {
        String msg = e.getMessage();
        return msg != null && msg.toLowerCase().contains("index already exists");
    }

    private static byte[] floatsToBytes(float[] vector) {
        ByteBuffer buf = ByteBuffer.allocate(Float.BYTES * vector.length).order(ByteOrder.LITTLE_ENDIAN);
        for (float f : vector) {
            buf.putFloat(f);
        }
        return buf.array();
    }

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private static long parseLong(Object v) {
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

    private static float parseFloat(Object v) {
        if (v == null) {
            return 0f;
        }
        if (v instanceof Number n) {
            return n.floatValue();
        }
        try {
            return Float.parseFloat(String.valueOf(v));
        } catch (Exception e) {
            return 0f;
        }
    }
}
