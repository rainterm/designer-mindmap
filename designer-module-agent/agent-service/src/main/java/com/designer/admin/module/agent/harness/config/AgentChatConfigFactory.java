package com.designer.admin.module.agent.harness.config;

import com.designer.admin.module.agent.service.provider.AgentVendorPresets;
import org.noear.solon.ai.chat.ChatConfig;

/**
 * 将供应商配置转为 Solon AI {@link ChatConfig}。
 * {@code type} 即方言（standard）；路径补全交给方言实现，不再手写 /chat/completions。
 *
 * @author 雨夜终端
 */
public final class AgentChatConfigFactory {

    /** 工具类，禁止实例化。 */
    private AgentChatConfigFactory() {
    }

    /**
     * 按供应商类型、地址与模型构造对话配置。
     *
     * @param type      供应商方言类型（如 openai / dashscope）
     * @param baseUrl   API Base URL，不可为空
     * @param apiKey    API Key，可空
     * @param modelCode 模型编码，可空（空则置为空白串）
     * @return 可用的 {@link ChatConfig}
     * @throws IllegalArgumentException Base URL 为空时抛出
     */
    public static ChatConfig fromProvider(String type, String baseUrl, String apiKey, String modelCode) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("供应商 Base URL 不能为空");
        }
        ChatConfig config = new ChatConfig();
        config.setStandard(AgentVendorPresets.toStandard(type));
        config.setApiUrl(baseUrl.trim());
        if (apiKey != null && !apiKey.isBlank()) {
            config.setApiKey(apiKey.trim());
        }
        config.setModel(modelCode == null ? "" : modelCode.trim());
        return config;
    }
}
