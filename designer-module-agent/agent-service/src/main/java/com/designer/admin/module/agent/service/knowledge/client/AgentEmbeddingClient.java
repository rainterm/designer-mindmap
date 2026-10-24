package com.designer.admin.module.agent.service.knowledge.client;

import com.designer.admin.common.exception.BizException;
import com.designer.admin.module.agent.service.provider.adapter.ModelListHttpSupport;
import com.designer.admin.module.agent.service.provider.adapter.ModelListParseSupport;
import com.designer.admin.module.agent.util.AgentJsonUtil;
import org.noear.snack4.ONode;
import org.noear.solon.annotation.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * OpenAI 兼容 Embeddings 客户端。
 *
 * @author 雨夜终端
 */
@Component
public class AgentEmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(AgentEmbeddingClient.class);
    private static final int TIMEOUT_SEC = 60;
    /** 单批上限：阿里云 text-embedding-v3/v4 等要求 ≤10，取兼容各厂商的保守值 */
    private static final int BATCH_SIZE = 10;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public List<float[]> embed(String baseUrl, String apiKey, String model, List<String> texts) {
        return embed(baseUrl, apiKey, model, texts, null);
    }

    /**
     * @param onProgress 可选回调 (已完成条数, 总条数)
     */
    public List<float[]> embed(String baseUrl, String apiKey, String model, List<String> texts,
                               BiConsumer<Integer, Integer> onProgress) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        if (model == null || model.isBlank()) {
            throw new BizException("Embedding 模型不能为空");
        }
        assertOpenAiCompatEmbedding(model.trim());
        String url = resolveEmbeddingsUrl(baseUrl);
        int total = texts.size();
        List<float[]> all = new ArrayList<>(total);
        for (int i = 0; i < total; i += BATCH_SIZE) {
            List<String> batch = texts.subList(i, Math.min(i + BATCH_SIZE, total));
            all.addAll(embedBatch(url, apiKey, model.trim(), batch));
            if (onProgress != null) {
                onProgress.accept(all.size(), total);
            }
        }
        return all;
    }

    public float[] embedOne(String baseUrl, String apiKey, String model, String text) {
        List<float[]> vectors = embed(baseUrl, apiKey, model, List.of(text == null ? "" : text));
        if (vectors.isEmpty()) {
            throw new BizException("Embedding 返回为空");
        }
        return vectors.get(0);
    }

    private List<float[]> embedBatch(String url, String apiKey, String model, List<String> batch) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        List<String> inputs = new ArrayList<>(batch.size());
        for (String t : batch) {
            inputs.add(t == null ? "" : t);
        }
        payload.put("input", inputs);
        String json = AgentJsonUtil.toJson(payload);
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(TIMEOUT_SEC))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
            if (apiKey != null && !apiKey.isBlank()) {
                builder.header("Authorization", "Bearer " + apiKey.trim());
            }
            HttpResponse<String> resp = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                throw new BizException(formatHttpError(resp.statusCode(), resp.body(), model));
            }
            return parseEmbeddings(resp.body(), batch.size());
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Embedding 调用失败 url={}", url, e);
            throw new BizException("Embedding 调用失败: " + e.getMessage(), e);
        }
    }

    /** 知识库仅支持 OpenAI 兼容文本向量；多模态 VL Embedding 需另走 DashScope。 */
    public static void assertOpenAiCompatEmbedding(String model) {
        if (ModelListParseSupport.isMultimodalEmbeddingModel(model)) {
            throw new BizException("模型「" + model + "」为多模态 Embedding，不支持 OpenAI 兼容 /embeddings。"
                    + "知识库请改用文本向量模型，例如 text-embedding-v4、text-embedding-v3");
        }
    }

    private static String formatHttpError(int status, String body, String model) {
        String msg = extractErrorMessage(body);
        String lower = (msg == null ? "" : msg).toLowerCase(Locale.ROOT);
        if (lower.contains("openai compatibility")
                || lower.contains("model_not_supported")
                || lower.contains("unsupported model")) {
            return "Embedding 模型「" + model + "」不支持 OpenAI 兼容接口。"
                    + "请改用文本向量模型（如 text-embedding-v4 / text-embedding-v3）。"
                    + (msg == null || msg.isBlank() ? "" : " 原错误: " + abbreviate(msg, 160));
        }
        return "Embedding HTTP " + status + ": " + abbreviate(msg != null && !msg.isBlank() ? msg : body, 200);
    }

    private static String extractErrorMessage(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            ONode root = ONode.ofJson(body);
            String m = root.get("error").get("message").getString();
            if (m != null && !m.isBlank()) {
                return m.trim();
            }
            m = root.get("message").getString();
            if (m != null && !m.isBlank()) {
                return m.trim();
            }
        } catch (Exception ignored) {
            // fall through
        }
        return body.trim();
    }

    private static List<float[]> parseEmbeddings(String body, int expect) {
        ONode root = ONode.ofJson(body == null ? "{}" : body);
        ONode data = root.get("data");
        if (!data.isArray() || data.size() == 0) {
            throw new BizException("Embedding 响应无 data");
        }
        float[][] ordered = new float[expect][];
        for (int i = 0; i < data.size(); i++) {
            ONode item = data.get(i);
            int idx = item.get("index").getInt(i);
            if (idx < 0 || idx >= expect) {
                continue;
            }
            ONode emb = item.get("embedding");
            if (!emb.isArray()) {
                throw new BizException("Embedding 向量格式错误");
            }
            float[] vec = new float[emb.size()];
            for (int j = 0; j < emb.size(); j++) {
                vec[j] = emb.get(j).getDouble(0d).floatValue();
            }
            ordered[idx] = vec;
        }
        List<float[]> out = new ArrayList<>(expect);
        for (int i = 0; i < expect; i++) {
            if (ordered[i] == null) {
                throw new BizException("Embedding 缺少 index=" + i);
            }
            out.add(ordered[i]);
        }
        return out;
    }

    static String resolveEmbeddingsUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new BizException("Embedding Provider Base URL 不能为空");
        }
        String root = ModelListHttpSupport.normalizeRoot(baseUrl);
        String lower = root.toLowerCase(Locale.ROOT);
        if (lower.endsWith("/embeddings")) {
            return root;
        }
        if (lower.matches(".*/v\\d+$")) {
            return root + "/embeddings";
        }
        if (lower.matches(".*/v\\d+/.*")) {
            int idx = lower.lastIndexOf("/v");
            int end = lower.indexOf('/', idx + 1);
            if (end < 0) {
                return root + "/embeddings";
            }
            return root.substring(0, end) + "/embeddings";
        }
        return root + "/v1/embeddings";
    }

    private static String abbreviate(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max) + "…";
    }
}
