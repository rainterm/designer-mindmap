package com.designer.admin.module.agent.workflow.activity;

import com.designer.admin.common.exception.BizException;
import com.designer.admin.module.agent.model.dto.workflow.node.HttpKeyValueDTO;
import com.designer.admin.module.agent.model.dto.workflow.node.HttpNodeConfigDTO;
import com.designer.admin.module.agent.util.AgentJsonUtil;
import org.noear.solon.Utils;
import org.noear.solon.annotation.Component;

import java.net.URI;
import java.net.URLEncoder;
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
import java.util.UUID;

import static com.designer.admin.module.agent.workflow.support.WorkflowVarSupport.abbreviate;
import static com.designer.admin.module.agent.workflow.support.WorkflowVarSupport.applyVars;
import static com.designer.admin.module.agent.workflow.support.WorkflowVarSupport.str;

/**
 * HTTP 活动节点执行。
 *
 * @author 雨夜终端
 */
@Component
public class WorkflowHttpActivity {

    /** 响应体写入工作流变量前的最大字符数，超出截断 */
    private static final int HTTP_BODY_LIMIT = 50_000;

    /** 共享 HTTP 客户端（短连接超时、跟随重定向） */
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /**
     * 发起 HTTP 请求：支持 GET/DELETE Query 拼表单、POST/PUT/PATCH 多种 Body，非 2xx 抛业务异常。
     *
     * @param config 节点 JSON 配置（method / url / timeoutSec / bodyType / headers / body / formFields 等）
     * @param vars   工作流变量表，用于替换 URL、头、Body 中的占位符
     * @return 响应正文（超长已截断）
     * @throws Exception 网络或请求构建失败时抛出；业务校验失败为 {@link BizException}
     */
    public String runHttp(String config, Map<String, String> vars) throws Exception {
        HttpNodeConfigDTO cfg = AgentJsonUtil.parse(config, HttpNodeConfigDTO.class);
        if (cfg == null) {
            cfg = new HttpNodeConfigDTO();
        }
        String method = Utils.isBlank(cfg.getMethod()) ? "GET" : cfg.getMethod().toUpperCase(Locale.ROOT);
        String url = applyVars(cfg.getUrl() == null ? "" : cfg.getUrl(), vars).trim();
        if (url.isBlank()) {
            throw new BizException("HTTP 节点未配置 URL");
        }
        int timeoutSec = cfg.getTimeoutSec() == null ? 15 : cfg.getTimeoutSec();
        if (timeoutSec < 1) {
            timeoutSec = 1;
        }
        if (timeoutSec > 120) {
            timeoutSec = 120;
        }
        String bodyType = Utils.isBlank(cfg.getBodyType()) ? "json" : cfg.getBodyType().trim().toLowerCase(Locale.ROOT);
        if (bodyType.isBlank()) {
            bodyType = "json";
        }

        boolean bodyMethod = isBodyMethod(method);
        // GET/DELETE 等：表单字段拼到 URL Query，而不是只能手写在链接上
        if (!bodyMethod) {
            url = appendQueryParams(url, resolveFormFields(cfg, vars));
        }

        Map<String, String> headerMap = resolveHeaders(cfg, vars);
        HttpBody encoded = bodyMethod
                ? encodeHttpBody(bodyType, cfg, vars, headerMap)
                : new HttpBody(new byte[0], null);
        if (encoded.contentType() != null && !hasHeaderIgnoreCase(headerMap, "Content-Type")) {
            headerMap.put("Content-Type", encoded.contentType());
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(timeoutSec));
        for (Map.Entry<String, String> e : headerMap.entrySet()) {
            if (e.getKey() == null || e.getKey().isBlank()) {
                continue;
            }
            builder.header(e.getKey(), e.getValue() == null ? "" : e.getValue());
        }

        HttpRequest.BodyPublisher publisher = encoded.bytes() == null || encoded.bytes().length == 0
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofByteArray(encoded.bytes());
        boolean hasBody = bodyMethod && encoded.bytes() != null && encoded.bytes().length > 0;
        switch (method) {
            case "GET":
                builder.GET();
                break;
            case "DELETE":
                builder.DELETE();
                break;
            case "POST":
                builder.POST(hasBody ? publisher : HttpRequest.BodyPublishers.noBody());
                break;
            case "PUT":
                builder.PUT(hasBody ? publisher : HttpRequest.BodyPublishers.noBody());
                break;
            case "PATCH":
                builder.method("PATCH", hasBody ? publisher : HttpRequest.BodyPublishers.noBody());
                break;
            default:
                throw new BizException("不支持的 HTTP 方法: " + method);
        }

        HttpResponse<String> resp = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String respBody = resp.body() == null ? "" : resp.body();
        if (respBody.length() > HTTP_BODY_LIMIT) {
            respBody = respBody.substring(0, HTTP_BODY_LIMIT) + "…(截断)";
        }
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new BizException("HTTP " + resp.statusCode() + ": " + abbreviate(respBody, 200));
        }
        return respBody;
    }

    /**
     * 是否为允许携带请求体的方法（POST / PUT / PATCH）。
     *
     * @param method 大写 HTTP 方法名
     * @return 是则 true
     */
    private static boolean isBodyMethod(String method) {
        return "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method);
    }

    /**
     * 将表单字段追加到 URL Query（保留原有 query / fragment）；供执行与步骤参数复用。
     *
     * @param url    原始 URL
     * @param fields 已解析的键值对列表
     * @return 拼接后的 URL；入参为空时原样或空串
     */
    public static String appendQueryParams(String url, List<Map.Entry<String, String>> fields) {
        if (url == null || url.isBlank() || fields == null || fields.isEmpty()) {
            return url == null ? "" : url;
        }
        String encoded = encodeUrlEncoded(fields);
        if (encoded.isBlank()) {
            return url;
        }
        int hash = url.indexOf('#');
        String base = hash >= 0 ? url.substring(0, hash) : url;
        String frag = hash >= 0 ? url.substring(hash) : "";
        String sep;
        if (base.contains("?")) {
            sep = (base.endsWith("?") || base.endsWith("&")) ? "" : "&";
        } else {
            sep = "?";
        }
        return base + sep + encoded + frag;
    }

    /** 编码后的请求体字节与建议 Content-Type */
    private record HttpBody(byte[] bytes, String contentType) {
    }

    /**
     * 解析请求头：优先 headerFields 表单，兼容旧的 headers 多行文本。
     *
     * @param cfg  节点配置
     * @param vars 变量表
     * @return 有序请求头 Map
     */
    public static Map<String, String> resolveHeaders(HttpNodeConfigDTO cfg, Map<String, String> vars) {
        Map<String, String> map = new LinkedHashMap<>();
        if (cfg == null) {
            return map;
        }
        List<HttpKeyValueDTO> headerFields = cfg.getHeaderFields();
        if (headerFields != null && !headerFields.isEmpty()) {
            for (HttpKeyValueDTO item : headerFields) {
                if (item == null) {
                    continue;
                }
                String key = applyVars(str(item.getKey(), "").trim(), vars).trim();
                if (key.isBlank()) {
                    continue;
                }
                String val = applyVars(str(item.getValue(), ""), vars);
                map.put(key, val);
            }
            return map;
        }
        return parseHeaderLines(applyVars(str(cfg.getHeaders(), ""), vars));
    }

    /**
     * 解析「Key: Value」多行请求头文本（# 行忽略）。
     *
     * @param headersRaw 多行文本
     * @return 有序请求头 Map
     */
    private static Map<String, String> parseHeaderLines(String headersRaw) {
        Map<String, String> map = new LinkedHashMap<>();
        if (headersRaw == null || headersRaw.isBlank()) {
            return map;
        }
        for (String line : headersRaw.split("\\r?\\n")) {
            String h = line.trim();
            if (h.isEmpty() || h.startsWith("#")) {
                continue;
            }
            int idx = h.indexOf(':');
            if (idx <= 0) {
                continue;
            }
            map.put(h.substring(0, idx).trim(), h.substring(idx + 1).trim());
        }
        return map;
    }

    /**
     * 忽略大小写判断是否已存在指定请求头。
     *
     * @param headers 当前请求头
     * @param name    头名称
     * @return 已存在则 true
     */
    private static boolean hasHeaderIgnoreCase(Map<String, String> headers, String name) {
        for (String k : headers.keySet()) {
            if (k != null && k.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 按 bodyType 编码请求体，并在未手动指定时给出默认 Content-Type。
     *
     * @param bodyType  none / form / multipart / raw / json 等
     * @param cfg       节点配置
     * @param vars      变量表
     * @param headers   已解析请求头（用于判断是否已有 Content-Type）
     * @return 编码结果
     */
    private HttpBody encodeHttpBody(String bodyType, HttpNodeConfigDTO cfg, Map<String, String> vars,
                                    Map<String, String> headers) {
        return switch (bodyType) {
            case "none", "empty" -> new HttpBody(new byte[0], null);
            case "form", "urlencoded", "x-www-form-urlencoded" -> {
                List<Map.Entry<String, String>> fields = resolveFormFields(cfg, vars);
                String encoded = encodeUrlEncoded(fields);
                yield new HttpBody(encoded.getBytes(StandardCharsets.UTF_8),
                        "application/x-www-form-urlencoded; charset=UTF-8");
            }
            case "multipart", "form-data" -> {
                List<Map.Entry<String, String>> fields = resolveFormFields(cfg, vars);
                String boundary = "----wfBoundary" + UUID.randomUUID().toString().replace("-", "");
                byte[] bytes = encodeMultipart(fields, boundary);
                yield new HttpBody(bytes, "multipart/form-data; boundary=" + boundary);
            }
            case "raw", "text" -> {
                String body = applyVars(str(cfg.getBody(), ""), vars);
                yield new HttpBody(body.getBytes(StandardCharsets.UTF_8),
                        hasHeaderIgnoreCase(headers, "Content-Type") ? null : "text/plain; charset=UTF-8");
            }
            default -> { // json
                String body = applyVars(str(cfg.getBody(), ""), vars);
                yield new HttpBody(body.getBytes(StandardCharsets.UTF_8),
                        hasHeaderIgnoreCase(headers, "Content-Type") ? null : "application/json; charset=UTF-8");
            }
        };
    }

    /**
     * 解析表单字段：优先 formFields；为空时兼容 body 中 key=value 多行。
     *
     * @param cfg  节点配置
     * @param vars 变量表（仅替换 value）
     * @return 有序键值对列表
     */
    public static List<Map.Entry<String, String>> resolveFormFields(HttpNodeConfigDTO cfg, Map<String, String> vars) {
        List<Map.Entry<String, String>> out = new ArrayList<>();
        if (cfg == null) {
            return out;
        }
        List<HttpKeyValueDTO> formFields = cfg.getFormFields();
        if (formFields != null) {
            for (HttpKeyValueDTO item : formFields) {
                if (item == null) {
                    continue;
                }
                String key = str(item.getKey(), "").trim();
                if (key.isBlank()) {
                    continue;
                }
                String val = applyVars(str(item.getValue(), ""), vars);
                out.add(Map.entry(key, val));
            }
        }
        // 兼容：body 里写 key=value 多行
        if (out.isEmpty()) {
            String body = applyVars(str(cfg.getBody(), ""), vars);
            for (String line : body.split("\\r?\\n")) {
                String t = line.trim();
                if (t.isEmpty() || t.startsWith("#")) {
                    continue;
                }
                int eq = t.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                out.add(Map.entry(t.substring(0, eq).trim(), t.substring(eq + 1).trim()));
            }
        }
        return out;
    }

    /**
     * 将字段列表编码为 application/x-www-form-urlencoded 串。
     *
     * @param fields 键值对
     * @return 编码串
     */
    private static String encodeUrlEncoded(List<Map.Entry<String, String>> fields) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : fields) {
            if (!sb.isEmpty()) {
                sb.append('&');
            }
            sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(e.getValue() == null ? "" : e.getValue(), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    /**
     * 将字段列表编码为 multipart/form-data 字节（仅文本 part）。
     *
     * @param fields   键值对
     * @param boundary 边界串
     * @return UTF-8 字节
     */
    private static byte[] encodeMultipart(List<Map.Entry<String, String>> fields, String boundary) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : fields) {
            sb.append("--").append(boundary).append("\r\n");
            sb.append("Content-Disposition: form-data; name=\"")
                    .append(escapeMultipartName(e.getKey()))
                    .append("\"\r\n\r\n");
            sb.append(e.getValue() == null ? "" : e.getValue()).append("\r\n");
        }
        sb.append("--").append(boundary).append("--\r\n");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 去掉 name 中的双引号，避免破坏 Content-Disposition。
     *
     * @param name 字段名
     * @return 安全后的字段名
     */
    private static String escapeMultipartName(String name) {
        if (name == null) {
            return "";
        }
        return name.replace("\"", "");
    }
}
