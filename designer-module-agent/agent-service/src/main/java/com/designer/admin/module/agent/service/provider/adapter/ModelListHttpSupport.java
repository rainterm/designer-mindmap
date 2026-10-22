package com.designer.admin.module.agent.service.provider.adapter;

import com.designer.admin.common.exception.BizException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 模型列表 HTTP 与 URL 规范化（供 {@code AgentProviderModelClient} 等复用）。
 *
 * @author 雨夜终端
 */
public final class ModelListHttpSupport {

    public static final int TIMEOUT_SEC = 20;

    private ModelListHttpSupport() {
    }

    public static String httpGet(HttpClient httpClient, String url, String apiKey) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(TIMEOUT_SEC))
                .header("Accept", "application/json")
                .GET();
        if (apiKey != null && !apiKey.isBlank()) {
            builder.header("Authorization", "Bearer " + apiKey.trim());
        }
        // 强制 UTF-8：避免个别网关 Content-Type 无 charset 时按平台默认编码解码导致中文变 ?
        HttpResponse<String> resp = httpClient.send(builder.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new BizException("HTTP " + resp.statusCode() + " from " + url);
        }
        return resp.body() == null ? "" : resp.body();
    }

    public static String normalizeRoot(String baseUrl) {
        String url = baseUrl.trim();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.endsWith("/chat/completions")) {
            return url.substring(0, url.length() - "/chat/completions".length());
        }
        if (lower.endsWith("/api/chat")) {
            return url.substring(0, url.length() - "/api/chat".length());
        }
        if (lower.endsWith("/completions")) {
            return url.substring(0, url.length() - "/completions".length());
        }
        return url;
    }

    public static List<String> buildOpenAiCandidateUrls(String root) {
        List<String> urls = new ArrayList<>();
        String r = root;
        while (r.endsWith("/")) {
            r = r.substring(0, r.length() - 1);
        }
        String lower = r.toLowerCase(Locale.ROOT);
        if (lower.matches(".*/v\\d+$")) {
            // /v1、/v3（火山方舟）、/v4（智谱）等
            urls.add(r + "/models");
        } else if (lower.matches(".*/v\\d+/.*")) {
            int idx = lower.lastIndexOf("/v");
            int end = lower.indexOf('/', idx + 1);
            if (end < 0) {
                urls.add(r + "/models");
            } else {
                urls.add(r.substring(0, end) + "/models");
            }
        } else {
            urls.add(r + "/v1/models");
            urls.add(r + "/models");
        }
        return urls.stream().distinct().toList();
    }

    public static List<String> buildOllamaCandidateUrls(String root) {
        List<String> urls = new ArrayList<>();
        String r = root;
        while (r.endsWith("/")) {
            r = r.substring(0, r.length() - 1);
        }
        String lower = r.toLowerCase(Locale.ROOT);
        String host = r;
        if (lower.endsWith("/v1")) {
            host = r.substring(0, r.length() - 3);
            while (host.endsWith("/")) {
                host = host.substring(0, host.length() - 1);
            }
        }
        urls.add(host + "/api/tags");
        urls.add(host + "/v1/models");
        urls.addAll(buildOpenAiCandidateUrls(r));
        return urls.stream().distinct().toList();
    }

    public static String aliyunMaasHost(String root) {
        if (root == null || root.isBlank()) {
            return null;
        }
        String r = root.trim();
        while (r.endsWith("/")) {
            r = r.substring(0, r.length() - 1);
        }
        String lower = r.toLowerCase(Locale.ROOT);
        if (!lower.contains("maas.aliyuncs.com") && !lower.contains("dashscope.aliyuncs.com")
                && !lower.contains("dashscope-intl.aliyuncs.com")) {
            return null;
        }
        int idx = lower.indexOf("/compatible-mode");
        if (idx > 0) {
            return r.substring(0, idx);
        }
        idx = lower.indexOf("/api/v1");
        if (idx > 0) {
            return r.substring(0, idx);
        }
        if (lower.endsWith("/v1")) {
            return r.substring(0, r.length() - 3);
        }
        return r;
    }
}
