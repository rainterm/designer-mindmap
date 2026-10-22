package com.designer.admin.module.agent.service.provider;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Solon AI 聊天方言预设表。{@code type} 直接存方言编码，不再做厂商归一。
 *
 * @author 雨夜终端
 */
public final class AgentVendorPresets {

    private static final String DEFAULT = "openai";

    /** 当前登记的 Solon 方言。 */
    private static final Set<String> STANDARDS = Set.of(
            "openai",
            "openai-responses",
            "ollama",
            "anthropic",
            "gemini",
            "gemini-interactions",
            "dashscope");

    private static final Map<String, AgentVendorPreset> BY_CODE = new LinkedHashMap<>();

    static {
        register(new AgentVendorPreset(
                "openai",
                "OpenAI 兼容",
                "https://api.openai.com/v1",
                "DeepSeek / Kimi / 智谱 / 硅基 / 火山 / 百炼 compatible-mode 等均选此项"));
        register(new AgentVendorPreset(
                "ollama",
                "Ollama",
                "http://127.0.0.1:11434",
                "本地或内网 Ollama；框架自动补全 /api/chat"));
        register(new AgentVendorPreset(
                "anthropic",
                "Anthropic",
                "https://api.anthropic.com",
                "Claude 原生 Messages API；若走 OpenAI 兼容网关请改选 OpenAI 兼容"));
        register(new AgentVendorPreset(
                "gemini",
                "Google Gemini",
                "https://generativelanguage.googleapis.com",
                "Gemini 原生接口；OpenAI 兼容代理请改选 OpenAI 兼容"));
        register(new AgentVendorPreset(
                "dashscope",
                "阿里云百炼（原生）",
                "https://dashscope.aliyuncs.com/api/v1/services",
                "DashScope 原生协议；compatible-mode 请改选 OpenAI 兼容"));
        register(new AgentVendorPreset(
                "openai-responses",
                "OpenAI Responses",
                "https://api.openai.com/v1",
                "OpenAI /v1/responses 协议"));
    }

    private AgentVendorPresets() {
    }

    private static void register(AgentVendorPreset preset) {
        BY_CODE.put(preset.code(), preset);
    }

    public static List<AgentVendorPreset> all() {
        return List.copyOf(BY_CODE.values());
    }

    public static AgentVendorPreset get(String code) {
        if (code == null || code.isBlank()) {
            return BY_CODE.get(DEFAULT);
        }
        return BY_CODE.get(toStandard(code));
    }

    public static boolean isRegistered(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        return BY_CODE.containsKey(toStandard(code));
    }

    public static AgentVendorPreset require(String code) {
        AgentVendorPreset p = get(code);
        return p != null ? p : BY_CODE.get(DEFAULT);
    }

    /**
     * 转为 Solon ChatConfig.standard。
     * 已是方言则原样返回；历史厂商编码（deepseek / aliyun_bailian 等）一律归为 openai（ollama 除外）。
     */
    public static String toStandard(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT;
        }
        String t = raw.trim().toLowerCase(Locale.ROOT);
        return switch (t) {
            case "openai", "openai_compat", "openai-compat", "custom" -> "openai";
            case "openai-responses", "openai_responses", "openai-response" -> "openai-responses";
            case "ollama" -> "ollama";
            case "anthropic", "claude" -> "anthropic";
            case "gemini", "google", "gemini-models" -> "gemini";
            case "gemini-interactions", "gemini_interactions" -> "gemini-interactions";
            case "dashscope" -> "dashscope";
            // 历史厂商编码：对话均走 OpenAI 兼容
            case "aliyun", "aliyun_bailian", "bailian", "qwen",
                 "deepseek", "moonshot", "kimi", "zhipu", "glm", "bigmodel",
                 "siliconflow", "silicon",
                 "volcengine_ark", "volcengine", "ark", "huoshan", "doubao" -> "openai";
            default -> STANDARDS.contains(t) ? t : DEFAULT;
        };
    }

    /** 列表筛选：方言 + 仍可能留在库里的旧 type 值。 */
    public static List<String> typesMatching(String standardOrRaw) {
        String standard = toStandard(standardOrRaw);
        List<String> types = new ArrayList<>();
        types.add(standard);
        if ("openai".equals(standard)) {
            types.addAll(List.of(
                    "openai_compat", "openai-compat", "custom",
                    "aliyun", "aliyun_bailian", "bailian", "qwen",
                    "deepseek", "moonshot", "kimi", "zhipu", "glm", "bigmodel",
                    "siliconflow", "silicon",
                    "volcengine_ark", "volcengine", "ark", "huoshan", "doubao"));
        } else if ("anthropic".equals(standard)) {
            types.add("claude");
        } else if ("gemini".equals(standard)) {
            types.addAll(List.of("google", "gemini-models"));
        }
        return types;
    }

    public static Map<String, Object> toMap(AgentVendorPreset p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", p.code());
        m.put("label", p.label());
        m.put("defaultBaseUrl", p.defaultBaseUrl());
        m.put("standard", p.code());
        m.put("hint", p.hint() == null ? "" : p.hint());
        return m;
    }
}
