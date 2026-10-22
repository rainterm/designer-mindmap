package com.designer.admin.module.agent.service.knowledge;

import com.designer.admin.common.exception.BizException;
import com.designer.admin.module.agent.service.provider.adapter.ModelListHttpSupport;
import com.designer.admin.module.agent.util.AgentJson;
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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Rerank 客户端。
 * <ul>
 *   <li>OpenAI 兼容：{@code POST .../v1/reranks}（阿里云 qwen3-rerank）</li>
 *   <li>阿里云原生：{@code POST .../api/v1/services/rerank/text-rerank/text-rerank}
 *       （gte-rerank-v2 / qwen3.7-text-rerank 等）</li>
 * </ul>
 *
 * @author 雨夜终端
 */
@Component
public class AgentRerankClient {

    private static final Logger log = LoggerFactory.getLogger(AgentRerankClient.class);
    private static final int TIMEOUT_SEC = 60;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /**
     * @param documents 待重排文档正文
     * @return 按相关性降序的原 documents 下标（长度不超过 topN）
     */
    public List<Integer> rerank(String baseUrl, String apiKey, String model,
                                String query, List<String> documents, int topN) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }
        if (model == null || model.isBlank()) {
            throw new BizException("Rerank 模型不能为空");
        }
        String modelCode = model.trim();
        int n = Math.max(1, Math.min(topN, documents.size()));
        List<Candidate> candidates = buildCandidates(baseUrl, modelCode, query, documents, n);
        Exception last = null;
        for (Candidate c : candidates) {
            try {
                HttpResponse<String> resp = postJson(c.url(), apiKey, c.bodyJson());
                if (resp.statusCode() == 404 || resp.statusCode() == 405) {
                    last = new BizException("Rerank HTTP " + resp.statusCode() + " @ " + c.url());
                    log.debug("Rerank 端点不可用: {}", last.getMessage());
                    continue;
                }
                if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                    throw new BizException("Rerank HTTP " + resp.statusCode() + ": "
                            + abbreviate(extractErrorMessage(resp.body()), 200));
                }
                log.debug("Rerank 成功 url={}", c.url());
                return parseResults(resp.body(), documents.size(), n);
            } catch (BizException e) {
                String msg = e.getMessage() == null ? "" : e.getMessage();
                if (msg.contains("HTTP 404") || msg.contains("HTTP 405")) {
                    last = e;
                    continue;
                }
                throw e;
            } catch (Exception e) {
                last = e;
                log.debug("Rerank 候选失败 url={}", c.url(), e);
            }
        }
        String tip = last == null ? "无可用端点" : last.getMessage();
        log.warn("Rerank 调用失败 baseUrl={} model={}: {}", baseUrl, modelCode, tip);
        throw new BizException("Rerank 调用失败: " + tip);
    }

    private HttpResponse<String> postJson(String url, String apiKey, String body) throws Exception {
        HttpRequest.Builder rb = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(TIMEOUT_SEC))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        if (apiKey != null && !apiKey.isBlank()) {
            rb.header("Authorization", "Bearer " + apiKey.trim());
        }
        return httpClient.send(rb.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static List<Candidate> buildCandidates(String baseUrl, String model, String query,
                                                   List<String> documents, int topN) {
        String openAiBody = AgentJson.toJson(openAiPayload(model, query, documents, topN));
        String dashBody = AgentJson.toJson(dashScopePayload(model, query, documents, topN));
        List<String> urls = resolveCandidateUrls(baseUrl, model);
        List<Candidate> out = new ArrayList<>();
        for (String url : urls) {
            String lower = url.toLowerCase(Locale.ROOT);
            if (lower.contains("/services/rerank/")) {
                out.add(new Candidate(url, dashBody));
            } else {
                out.add(new Candidate(url, openAiBody));
            }
        }
        return out;
    }

    private static Map<String, Object> openAiPayload(String model, String query,
                                                     List<String> documents, int topN) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("query", query == null ? "" : query);
        payload.put("documents", documents);
        payload.put("top_n", topN);
        return payload;
    }

    private static Map<String, Object> dashScopePayload(String model, String query,
                                                        List<String> documents, int topN) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("query", query == null ? "" : query);
        input.put("documents", documents);
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("top_n", topN);
        parameters.put("return_documents", false);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("input", input);
        payload.put("parameters", parameters);
        return payload;
    }

    static List<String> resolveCandidateUrls(String baseUrl, String model) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new BizException("Rerank Provider Base URL 不能为空");
        }
        String root = ModelListHttpSupport.normalizeRoot(baseUrl);
        String lower = root.toLowerCase(Locale.ROOT);
        Set<String> urls = new LinkedHashSet<>();

        if (lower.endsWith("/reranks") || lower.endsWith("/rerank")
                || lower.contains("/services/rerank/")) {
            urls.add(root);
            return new ArrayList<>(urls);
        }

        boolean aliyun = lower.contains("maas.aliyuncs.com")
                || lower.contains("dashscope.aliyuncs.com")
                || lower.contains("dashscope-intl.aliyuncs.com");
        String hostRoot = ModelListHttpSupport.aliyunMaasHost(root);
        if (hostRoot == null && aliyun) {
            hostRoot = stripToHostRoot(root);
        }

        // OpenAI 兼容端点：文档为 /compatible-api/v1/reranks；存量多为 /compatible-mode/v1
        for (String compat : compatBases(root, hostRoot)) {
            urls.add(compat + "/reranks");
            urls.add(compat + "/rerank");
        }

        // 阿里云原生 text-rerank
        if (hostRoot != null) {
            String nativeUrl = hostRoot + "/api/v1/services/rerank/text-rerank/text-rerank";
            // gte / qwen3.7-text-rerank 等优先原生；qwen3-rerank 优先兼容
            String modelLower = model == null ? "" : model.toLowerCase(Locale.ROOT);
            boolean preferNative = model != null
                    && !"qwen3-rerank".equals(modelLower)
                    && !modelLower.startsWith("qwen3-rerank");
            if (preferNative) {
                List<String> ordered = new ArrayList<>();
                ordered.add(nativeUrl);
                ordered.addAll(urls);
                return ordered;
            }
            urls.add(nativeUrl);
        }

        if (urls.isEmpty()) {
            urls.add(root + "/v1/reranks");
            urls.add(root + "/v1/rerank");
        }
        return new ArrayList<>(urls);
    }

    private static List<String> compatBases(String root, String hostRoot) {
        LinkedHashSet<String> bases = new LinkedHashSet<>();
        String lower = root.toLowerCase(Locale.ROOT);
        if (lower.contains("/compatible-mode") || lower.contains("/compatible-api")) {
            int idxMode = lower.indexOf("/compatible-mode");
            int idxApi = lower.indexOf("/compatible-api");
            int idx = idxMode >= 0 ? idxMode : idxApi;
            String prefix = root.substring(0, idx);
            // 文档路径优先
            bases.add(prefix + "/compatible-api/v1");
            bases.add(prefix + "/compatible-mode/v1");
            return new ArrayList<>(bases);
        }
        if (lower.matches(".*/v\\d+$")) {
            bases.add(root);
            return new ArrayList<>(bases);
        }
        if (hostRoot != null) {
            bases.add(hostRoot + "/compatible-api/v1");
            bases.add(hostRoot + "/compatible-mode/v1");
            return new ArrayList<>(bases);
        }
        bases.add(root + "/v1");
        return new ArrayList<>(bases);
    }

    private static String stripToHostRoot(String root) {
        String lower = root.toLowerCase(Locale.ROOT);
        int idx = lower.indexOf("/compatible-");
        if (idx > 0) {
            return root.substring(0, idx);
        }
        idx = lower.indexOf("/api/v1");
        if (idx > 0) {
            return root.substring(0, idx);
        }
        if (lower.endsWith("/v1")) {
            return root.substring(0, root.length() - 3);
        }
        return root;
    }

    private static List<Integer> parseResults(String body, int docCount, int topN) {
        ONode root = ONode.ofJson(body == null ? "{}" : body);
        ONode results = root.get("results");
        if (!results.isArray() || results.size() == 0) {
            results = root.get("output").get("results");
        }
        if (!results.isArray() || results.size() == 0) {
            results = root.get("data");
        }
        if (!results.isArray() || results.size() == 0) {
            throw new BizException("Rerank 响应无 results");
        }
        List<ScoredIndex> scored = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            ONode item = results.get(i);
            int idx = item.get("index").getInt(-1);
            if (idx < 0) {
                idx = item.get("document_index").getInt(-1);
            }
            if (idx < 0 || idx >= docCount) {
                continue;
            }
            double score = item.get("relevance_score").getDouble(
                    item.get("score").getDouble(0d));
            scored.add(new ScoredIndex(idx, score));
        }
        if (scored.isEmpty()) {
            throw new BizException("Rerank 结果为空");
        }
        scored.sort(Comparator.comparingDouble(ScoredIndex::score).reversed());
        int limit = Math.max(1, Math.min(topN, scored.size()));
        List<Integer> out = new ArrayList<>(limit);
        for (int i = 0; i < limit; i++) {
            out.add(scored.get(i).index());
        }
        return out;
    }

    private static String extractErrorMessage(String body) {
        if (body == null || body.isBlank()) {
            return "";
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

    private static String abbreviate(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max) + "…";
    }

    private record Candidate(String url, String bodyJson) {
    }

    private record ScoredIndex(int index, double score) {
    }
}
