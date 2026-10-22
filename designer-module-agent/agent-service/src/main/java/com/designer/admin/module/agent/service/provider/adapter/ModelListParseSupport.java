package com.designer.admin.module.agent.service.provider.adapter;

import org.noear.snack4.ONode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 模型列表 JSON 解析与类型/上下文提取（优先接口元数据；阿里云 TR 等歧义时辅以模型名）。
 *
 * @author 雨夜终端
 */
public final class ModelListParseSupport {

    private ModelListParseSupport() {
    }

    public static List<Map<String, Object>> parseOpenAiOrOllamaBody(String body) {
        if (body == null || body.isBlank()) {
            return List.of();
        }
        ONode root = ONode.ofJson(body);
        List<Map<String, Object>> result = new ArrayList<>();

        ONode data = root.get("data");
        if (data != null && data.isArray() && data.size() > 0) {
            for (int i = 0; i < data.size(); i++) {
                ONode item = data.get(i);
                String id = text(item, "id");
                if (id == null || id.isBlank()) {
                    continue;
                }
                result.add(modelRow(id, id, contextWindow(item), resolveModelType(item), 1));
            }
            return result;
        }

        ONode models = root.get("models");
        if (models != null && models.isArray() && models.size() > 0) {
            for (int i = 0; i < models.size(); i++) {
                ONode item = models.get(i);
                String name = firstNonBlank(text(item, "name"), text(item, "model"));
                if (name == null || name.isBlank()) {
                    continue;
                }
                result.add(modelRow(name, name, contextWindow(item), resolveModelType(item), 1));
            }
        }
        return result;
    }

    public static List<Map<String, Object>> parseAliyunModels(ONode models) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (models == null || !models.isArray() || models.size() == 0) {
            return result;
        }
        for (int i = 0; i < models.size(); i++) {
            ONode item = models.get(i);
            String code = firstNonBlank(text(item, "model"), text(item, "id"), text(item, "name"));
            if (code == null || code.isBlank()) {
                continue;
            }
            String display = firstNonBlank(text(item, "name"), code);
            result.add(modelRow(code, display, contextWindow(item), resolveModelType(item), 1));
        }
        return result;
    }

    public static Map<String, Object> modelRow(String code, String display, Integer contextWindow,
                                               String modelType, int status) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("modelCode", code);
        row.put("displayName", display);
        row.put("contextWindow", contextWindow != null && contextWindow > 0 ? contextWindow : null);
        row.put("modelType", modelType);
        row.put("status", status);
        return row;
    }

    public static String resolveModelType(ONode item) {
        String modelId = firstNonBlank(text(item, "id"), text(item, "model"), text(item, "name"));
        String displayName = firstNonBlank(text(item, "name"), text(item, "display_name"), text(item, "displayName"));
        // 多模态 VL Embedding 不能走 OpenAI 兼容 embeddings，勿标成 embedding
        if (isMultimodalEmbeddingModel(modelId)) {
            return "multimodal";
        }
        // 阿里云 embedding/rerank 均标 capabilities=TR，须靠模型名区分
        if (isRerankModelName(modelId) || isRerankModelName(displayName)) {
            return "rerank";
        }
        String fromApi = firstNonBlank(
                meaningfulTypeField(item, "model_type"),
                meaningfulTypeField(item, "modelType"),
                meaningfulTypeField(item, "task_type"),
                meaningfulTypeField(item, "taskType"),
                meaningfulTypeField(item, "type"));
        String normalized = normalizeModelType(fromApi);
        if (normalized != null) {
            // API 偶发把 rerank 标成 embedding/text，名称含 rerank 时纠偏
            if ("embedding".equals(normalized)
                    && (isRerankModelName(modelId) || isRerankModelName(displayName))) {
                return "rerank";
            }
            return normalized;
        }
        String fromAliyunCaps = typeFromAliyunCapabilities(item);
        if (fromAliyunCaps != null) {
            if ("embedding".equals(fromAliyunCaps)
                    && (isRerankModelName(modelId) || isRerankModelName(displayName))) {
                return "rerank";
            }
            return fromAliyunCaps;
        }
        String fromEndpoint = typeFromSupportedEndpoints(item);
        if (fromEndpoint != null) {
            return fromEndpoint;
        }
        String fromArch = typeFromArchitecture(item);
        if (fromArch != null) {
            return fromArch;
        }
        String fromCaps = typeFromCapabilities(item);
        if (fromCaps != null) {
            return fromCaps;
        }
        // OpenAI 兼容列表常无类型字段，按模型名兜底
        if (isTextEmbeddingModelName(modelId) || isTextEmbeddingModelName(displayName)) {
            return "embedding";
        }
        return null;
    }

    /** 文本重排序模型（gte-rerank / qwen3-rerank 等）。 */
    public static boolean isRerankModelName(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        String m = name.toLowerCase(Locale.ROOT);
        return m.contains("rerank") || m.contains("re-rank") || m.contains("re_rank")
                || m.contains("重排序");
    }

    /** 文本向量模型名（排除 VL / rerank）。 */
    public static boolean isTextEmbeddingModelName(String name) {
        if (name == null || name.isBlank() || isMultimodalEmbeddingModel(name) || isRerankModelName(name)) {
            return false;
        }
        String m = name.toLowerCase(Locale.ROOT);
        return m.contains("embedding") || m.contains("text-embed") || m.contains("bge-m3");
    }

    /** 阿里云等平台的多模态向量模型（需 DashScope multimodal-embedding，非 OpenAI /embeddings）。 */
    public static boolean isMultimodalEmbeddingModel(String modelCode) {
        if (modelCode == null || modelCode.isBlank()) {
            return false;
        }
        String m = modelCode.trim().toLowerCase(Locale.ROOT);
        return m.contains("vl-embedding")
                || m.contains("embedding-vision")
                || m.contains("tongyi-embedding-vision")
                || m.contains("multimodal-embedding");
    }

    private static String typeFromAliyunCapabilities(ONode item) {
        if (item == null || !item.isObject()) {
            return null;
        }
        ONode caps = item.get("capabilities");
        if (caps == null || !caps.isArray() || caps.size() == 0) {
            return null;
        }
        boolean tg = false;
        boolean vu = false;
        boolean ig = false;
        boolean asr = false;
        boolean tts = false;
        boolean textEmbed = false;
        boolean multiEmbed = false;
        boolean omni = false;
        for (int i = 0; i < caps.size(); i++) {
            ONode n = caps.get(i);
            if (n == null || n.isNull()) {
                continue;
            }
            String c = n.getString();
            if (c == null || c.isBlank()) {
                continue;
            }
            String u = c.trim().toUpperCase(Locale.ROOT);
            switch (u) {
                case "TG":
                case "REASONING":
                    tg = true;
                    break;
                case "VU":
                case "MULTIMODAL-OMNI":
                case "REALTIME-OMNI":
                case "REALTIME-CHATTING":
                    vu = true;
                    omni = u.contains("OMNI") || u.contains("REALTIME");
                    break;
                case "IG":
                case "VG":
                case "3D-GENERATION":
                    ig = true;
                    break;
                case "ASR":
                case "REALTIME-ASR":
                case "REALTIME-AUDIO-TRANSLATE":
                    asr = true;
                    break;
                case "TTS":
                case "REALTIME-TEXT-TO-SPEECH":
                    tts = true;
                    break;
                // TR=文本向量；ME=多模态向量（不走 OpenAI /embeddings）
                case "TR":
                    textEmbed = true;
                    break;
                case "ME":
                    multiEmbed = true;
                    break;
                default:
                    break;
            }
        }
        if (ig) {
            return "image";
        }
        if (asr) {
            return "speech";
        }
        if (tts) {
            return "tts";
        }
        if (textEmbed) {
            return "embedding";
        }
        if (multiEmbed || vu || omni) {
            return "multimodal";
        }
        if (tg) {
            return "chat";
        }
        return null;
    }

    private static String meaningfulTypeField(ONode item, String key) {
        String v = text(item, key);
        if (v == null || v.isBlank()) {
            return null;
        }
        String t = v.trim().toLowerCase(Locale.ROOT);
        if ("model".equals(t) || "list".equals(t) || "object".equals(t)) {
            return null;
        }
        return v;
    }

    private static String typeFromSupportedEndpoints(ONode item) {
        if (item == null || !item.isObject()) {
            return null;
        }
        ONode node = item.get("supported_endpoint_types");
        if (node == null || node.isNull()) {
            node = item.get("supportedEndpointTypes");
        }
        if (node == null || node.isNull()) {
            return null;
        }
        String blob = node.toString().toLowerCase(Locale.ROOT);
        if (containsAny(blob, "image-generation", "image_generation", "images", "mj-", "midjourney")) {
            return "image";
        }
        if (containsAny(blob, "embedding", "embeddings")) {
            return "embedding";
        }
        if (containsAny(blob, "rerank")) {
            return "rerank";
        }
        if (containsAny(blob, "audio-speech", "audio.speech", "tts")) {
            return "tts";
        }
        if (containsAny(blob, "audio-transcription", "transcription", "asr", "speech-to-text")) {
            return "speech";
        }
        if (containsAny(blob, "moderation")) {
            return "moderation";
        }
        if (containsAny(blob, "openai", "anthropic", "gemini", "openai-response", "chat")) {
            return "chat";
        }
        return null;
    }

    private static String typeFromArchitecture(ONode item) {
        if (item == null || !item.isObject()) {
            return null;
        }
        ONode arch = item.get("architecture");
        if (arch == null || !arch.isObject()) {
            arch = item;
        }
        ONode meta = item.get("inference_metadata");
        String modality = firstNonBlank(text(arch, "modality"), text(item, "modality"));
        String in = firstNonBlank(
                nodeToLowerBlob(arch.get("input_modalities")),
                nodeToLowerBlob(arch.get("inputModalities")),
                nodeToLowerBlob(item.get("input_modalities")),
                meta == null ? null : nodeToLowerBlob(meta.get("request_modality")));
        String out = firstNonBlank(
                nodeToLowerBlob(arch.get("output_modalities")),
                nodeToLowerBlob(arch.get("outputModalities")),
                nodeToLowerBlob(item.get("output_modalities")),
                meta == null ? null : nodeToLowerBlob(meta.get("response_modality")));
        String blob = ((modality == null ? "" : modality) + " " + (in == null ? "" : in) + " "
                + (out == null ? "" : out)).toLowerCase(Locale.ROOT).trim();
        if (blob.isEmpty()) {
            return null;
        }
        if (blob.contains("embed")) {
            return "embedding";
        }
        if (out != null && (out.contains("image") || out.contains("video"))
                && (in == null || (!in.contains("image") && !in.contains("video")))) {
            return "image";
        }
        if (blob.contains("->image") || blob.contains("text->image")) {
            return "image";
        }
        if ((in != null && (in.contains("image") || in.contains("audio") || in.contains("video")))
                || blob.contains("text+image") || blob.contains("image->") || blob.contains("vision")) {
            return "multimodal";
        }
        if (blob.contains("text")) {
            return "chat";
        }
        return null;
    }

    private static String typeFromCapabilities(ONode item) {
        if (item == null || !item.isObject()) {
            return null;
        }
        for (String key : List.of("capabilities", "capability", "tags", "modes", "features")) {
            ONode node = item.get(key);
            if (node == null || node.isNull()) {
                continue;
            }
            String blob = node.toString().toLowerCase(Locale.ROOT);
            if (blob.contains("embed")) {
                return "embedding";
            }
            if (blob.contains("rerank")) {
                return "rerank";
            }
            if (blob.contains("speech") || blob.contains("transcrib") || blob.contains("asr")) {
                return "speech";
            }
            if (blob.contains("tts") || blob.contains("audio.speech")) {
                return "tts";
            }
            if (blob.contains("moderat")) {
                return "moderation";
            }
            if (containsAny(blob, "image.generation", "image_generation", "dall", "text-to-image")) {
                return "image";
            }
            if (containsAny(blob, "multimodal", "vision", "image")) {
                return "multimodal";
            }
            if (containsAny(blob, "chat", "completion", "llm")) {
                return "chat";
            }
        }
        return null;
    }

    public static String normalizeModelType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String t = raw.trim().toLowerCase(Locale.ROOT);
        return switch (t) {
            case "chat", "llm", "text", "language", "completion", "completions",
                 "chat.completion", "chat_completion", "text-generation", "text_generation" -> "chat";
            case "multimodal", "multi-modal", "multi_modal", "vl", "vlm", "vision",
                 "vision-language", "vision_language" -> "multimodal";
            case "embedding", "embeddings", "embed", "text-embedding", "text_embedding" -> "embedding";
            case "rerank", "reranker", "re-rank", "re_rank" -> "rerank";
            case "speech", "asr", "whisper", "audio.transcription", "transcription", "stt" -> "speech";
            case "tts", "audio.speech", "text-to-speech", "text_to_speech" -> "tts";
            case "image", "images", "image.generation", "image_generation", "dall-e", "text-to-image" -> "image";
            case "moderation", "moderations" -> "moderation";
            default -> null;
        };
    }

    public static Integer contextWindow(ONode item) {
        Integer n = intVal(item, "context_window");
        if (n == null) {
            n = intVal(item, "context_length");
        }
        if (n == null) {
            n = intVal(item, "max_model_len");
        }
        if (n == null) {
            ONode modelInfo = item.get("model_info");
            if (modelInfo != null && modelInfo.isObject()) {
                n = intVal(modelInfo, "context_window");
            }
        }
        if (n == null) {
            ONode meta = item.get("meta");
            if (meta != null && meta.isObject()) {
                n = intVal(meta, "max_model_len");
            }
        }
        return n != null && n > 0 ? n : null;
    }

    public static Integer intVal(ONode node, String key) {
        if (node == null || !node.isObject()) {
            return null;
        }
        ONode v = node.get(key);
        if (v == null || v.isNull()) {
            return null;
        }
        try {
            return v.getInt();
        } catch (Exception e) {
            try {
                return Integer.parseInt(v.getString());
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    public static String text(ONode node, String key) {
        if (node == null || !node.isObject()) {
            return null;
        }
        ONode v = node.get(key);
        if (v == null || v.isNull()) {
            return null;
        }
        String s = v.getString();
        return s == null ? null : s.trim();
    }

    public static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    private static String nodeToLowerBlob(ONode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return node.toString().toLowerCase(Locale.ROOT);
    }

    private static boolean containsAny(String haystack, String... needles) {
        for (String n : needles) {
            if (haystack.contains(n)) {
                return true;
            }
        }
        return false;
    }
}
