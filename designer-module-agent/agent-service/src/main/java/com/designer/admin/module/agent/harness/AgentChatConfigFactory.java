package com.designer.admin.module.agent.harness;

import com.designer.admin.module.agent.service.provider.AgentVendorPresets;
import org.noear.solon.ai.chat.ChatConfig;

/**
 * 将供应商配置转为 Solon AI {@link ChatConfig}。
 * {@code type} 即方言（standard）；路径补全交给方言实现，不再手写 /chat/completions。
 *
 * @author 雨夜终端
 */
public final class AgentChatConfigFactory {

    private AgentChatConfigFactory() {
    }

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
