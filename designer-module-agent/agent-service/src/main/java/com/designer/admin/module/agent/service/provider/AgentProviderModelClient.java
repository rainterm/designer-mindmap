package com.designer.admin.module.agent.service.provider;

import com.designer.admin.common.exception.BizException;
import com.designer.admin.module.agent.service.provider.adapter.ModelListHttpSupport;
import com.designer.admin.module.agent.service.provider.adapter.ModelListParseSupport;
import org.noear.snack4.ONode;
import org.noear.solon.annotation.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 拉取远端模型列表：按 Solon 方言（及阿里 URL 特征）分支，无扩展 SPI。
 *
 * @author 雨夜终端
 */
@Component
public class AgentProviderModelClient {

    private static final Logger log = LoggerFactory.getLogger(AgentProviderModelClient.class);
    private static final int ALIYUN_PAGE_SIZE = 100;
    private static final int ALIYUN_MAX_PAGES = 30;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public List<Map<String, Object>> fetchModels(String type, String baseUrl, String apiKey) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new BizException("Base URL 不能为空");
        }
        String standard = AgentVendorPresets.toStandard(type);
        String url = baseUrl.trim();
        try {
            List<Map<String, Object>> models;
            if (isAliyun(standard, url)) {
                models = listAliyun(url, apiKey);
            } else if ("ollama".equals(standard) || looksLikeOllama(url)) {
                models = listByCandidateUrls(ModelListHttpSupport.buildOllamaCandidateUrls(
                        ModelListHttpSupport.normalizeRoot(url)), apiKey, "ollama");
            } else {
                models = listByCandidateUrls(ModelListHttpSupport.buildOpenAiCandidateUrls(
                        ModelListHttpSupport.normalizeRoot(url)), apiKey, "openai");
            }
            if (models == null || models.isEmpty()) {
                throw new BizException("未返回任何模型");
            }
            models.sort(Comparator.comparing(m -> String.valueOf(m.getOrDefault("modelCode", "")),
                    String.CASE_INSENSITIVE_ORDER));
            return models;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("拉取模型失败 standard={}", standard, e);
            throw new BizException("拉取模型失败: " + e.getMessage(), e);
        }
    }

    private static boolean isAliyun(String standard, String baseUrl) {
        if ("dashscope".equalsIgnoreCase(standard)) {
            return true;
        }
        String lower = baseUrl == null ? "" : baseUrl.toLowerCase(Locale.ROOT);
        return lower.contains("maas.aliyuncs.com")
                || lower.contains("dashscope.aliyuncs.com")
                || lower.contains("dashscope-intl.aliyuncs.com");
    }

    private static boolean looksLikeOllama(String baseUrl) {
        String lower = baseUrl == null ? "" : baseUrl.toLowerCase(Locale.ROOT);
        return lower.contains("11434") || lower.contains("ollama");
    }

    private List<Map<String, Object>> listByCandidateUrls(List<String> urls, String apiKey, String tag)
            throws Exception {
        Exception last = null;
        for (String candidate : urls) {
            try {
                String body = ModelListHttpSupport.httpGet(httpClient, candidate, apiKey);
                List<Map<String, Object>> models = ModelListParseSupport.parseOpenAiOrOllamaBody(body);
                if (!models.isEmpty()) {
                    return models;
                }
            } catch (Exception e) {
                last = e;
                log.debug("{} 拉取失败 url={}", tag, candidate, e);
            }
        }
        String tip = last == null ? "未返回任何模型" : last.getMessage();
        throw new BizException("拉取模型失败: " + tip, last);
    }

    private List<Map<String, Object>> listAliyun(String baseUrl, String apiKey) throws Exception {
        String root = ModelListHttpSupport.normalizeRoot(baseUrl);
        String host = ModelListHttpSupport.aliyunMaasHost(root);
        if (host == null) {
            if (root.contains("{WorkspaceId}") || root.contains("{workspaceId}")) {
                throw new BizException("请先将 Base URL 中的 {WorkspaceId} 替换为业务空间 ID");
            }
            throw new BizException("无法解析阿里云 MaaS 主机地址");
        }
        Exception last = null;
        try {
            List<Map<String, Object>> models = fetchAliyunPaged(host, apiKey);
            if (!models.isEmpty()) {
                return models;
            }
        } catch (Exception e) {
            last = e;
            log.debug("阿里云原生列表失败 host={}", host, e);
        }
        try {
            return listByCandidateUrls(ModelListHttpSupport.buildOpenAiCandidateUrls(root), apiKey, "aliyun-compat");
        } catch (BizException e) {
            if (last != null) {
                throw new BizException("拉取模型失败: " + last.getMessage(), last);
            }
            throw e;
        }
    }

    private List<Map<String, Object>> fetchAliyunPaged(String hostRoot, String apiKey) throws Exception {
        List<Map<String, Object>> all = new ArrayList<>();
        int total = -1;
        for (int page = 1; page <= ALIYUN_MAX_PAGES; page++) {
            String url = hostRoot + "/api/v1/models?page_no=" + page + "&page_size=" + ALIYUN_PAGE_SIZE;
            String body = ModelListHttpSupport.httpGet(httpClient, url, apiKey);
            ONode root = ONode.ofJson(body);
            ONode output = root.get("output");
            if (output == null || !output.isObject()) {
                break;
            }
            if (total < 0) {
                Integer t = ModelListParseSupport.intVal(output, "total");
                total = t == null ? -1 : t;
            }
            List<Map<String, Object>> pageRows = ModelListParseSupport.parseAliyunModels(output.get("models"));
            if (pageRows.isEmpty()) {
                break;
            }
            all.addAll(pageRows);
            if (total >= 0 && all.size() >= total) {
                break;
            }
            if (pageRows.size() < ALIYUN_PAGE_SIZE) {
                break;
            }
        }
        return all;
    }
}
