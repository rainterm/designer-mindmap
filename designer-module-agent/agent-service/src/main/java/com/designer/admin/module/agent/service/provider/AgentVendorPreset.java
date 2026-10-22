package com.designer.admin.module.agent.service.provider;

/**
 * Solon AI 聊天方言预设：新建供应商时带出默认 Base URL。
 * {@code code} 即 ChatConfig.standard（openai / ollama / anthropic 等）。
 *
 * @author 雨夜终端
 */
public record AgentVendorPreset(
        String code,
        String label,
        String defaultBaseUrl,
        String hint
) {
}
