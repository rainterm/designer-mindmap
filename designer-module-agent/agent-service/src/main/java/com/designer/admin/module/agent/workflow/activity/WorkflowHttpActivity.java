package com.designer.admin.module.agent.workflow.activity;

import com.designer.admin.common.exception.BizException;
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
import static com.designer.admin.module.agent.workflow.support.WorkflowVarSupport.intVal;
import static com.designer.admin.module.agent.workflow.support.WorkflowVarSupport.parseCfg;
import static com.designer.admin.module.agent.workflow.support.WorkflowVarSupport.str;
/**
 * HTTP 活动节点执行。
 *
 * @author 雨夜终端
 */
@Component
public class WorkflowHttpActivity {

    private static final int HTTP_BODY_LIMIT = 50_000;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public String runHttp(String config, Map<String, String> vars) throws Exception {
        Map<String, Object> cfg = parseCfg(config);
        String method = str(cfg.get("method"), "GET").toUpperCase(Locale.ROOT);
        String url = applyVars(str(cfg.get("url"), ""), vars).trim();
        if (url.isBlank()) {
            throw new BizException("HTTP 节点未配置 URL");
        }
        int timeoutSec = intVal(cfg.get("timeoutSec"), 15);
        if (timeoutSec < 1) {
            timeoutSec = 1;
        }
        if (timeoutSec > 120) {
            timeoutSec = 120;
        }
        String bodyType = str(cfg.get("bodyType"), "json").trim().toLowerCase(Locale.ROOT);
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

    private static boolean isBodyMethod(String method) {
        return "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method);
    }

    /** 将表单字段追加到 URL Query（保留原有 query / fragment）；供执行与步骤参数复用 */
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

    private record HttpBody(byte[] bytes, String contentType) {
    }

    /** 请求头：优先 headerFields 表单，兼容旧的 headers 多行文本 */
    public static Map<String, String> resolveHeaders(Map<String, Object> cfg, Map<String, String> vars) {
        Map<String, String> map = new LinkedHashMap<>();
        if (cfg == null) {
            return map;
        }
        Object raw = cfg.get("headerFields");
        if (raw instanceof List<?> list && !list.isEmpty()) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> m)) {
                    continue;
                }
                String key = applyVars(str(m.get("key"), "").trim(), vars).trim();
                if (key.isBlank()) {
                    continue;
                }
                String val = applyVars(str(m.get("value"), ""), vars);
                map.put(key, val);
            }
            return map;
        }
        return parseHeaderLines(applyVars(str(cfg.get("headers"), ""), vars));
    }

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

    private static boolean hasHeaderIgnoreCase(Map<String, String> headers, String name) {
        for (String k : headers.keySet()) {
            if (k != null && k.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private HttpBody encodeHttpBody(String bodyType, Map<String, Object> cfg, Map<String, String> vars,
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
                String body = applyVars(str(cfg.get("body"), ""), vars);
                yield new HttpBody(body.getBytes(StandardCharsets.UTF_8),
                        hasHeaderIgnoreCase(headers, "Content-Type") ? null : "text/plain; charset=UTF-8");
            }
            default -> { // json
                String body = applyVars(str(cfg.get("body"), ""), vars);
                yield new HttpBody(body.getBytes(StandardCharsets.UTF_8),
                        hasHeaderIgnoreCase(headers, "Content-Type") ? null : "application/json; charset=UTF-8");
            }
        };
    }

    public static List<Map.Entry<String, String>> resolveFormFields(Map<String, Object> cfg, Map<String, String> vars) {
        List<Map.Entry<String, String>> out = new ArrayList<>();
        Object raw = cfg.get("formFields");
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> m)) {
                    continue;
                }
                String key = str(m.get("key"), "").trim();
                if (key.isBlank()) {
                    continue;
                }
                String val = applyVars(str(m.get("value"), ""), vars);
                out.add(Map.entry(key, val));
            }
        }
        // 兼容：body 里写 key=value 多行
        if (out.isEmpty()) {
            String body = applyVars(str(cfg.get("body"), ""), vars);
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

    private static String escapeMultipartName(String name) {
        if (name == null) {
            return "";
        }
        return name.replace("\"", "");
    }
}
