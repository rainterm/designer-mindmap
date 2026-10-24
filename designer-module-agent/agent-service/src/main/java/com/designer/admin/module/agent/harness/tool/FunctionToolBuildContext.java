package com.designer.admin.module.agent.harness.tool;

import com.designer.admin.module.agent.model.entity.agent.AgentAgent;
import org.noear.solon.ai.chat.ChatConfig;

import java.util.List;
import java.util.function.Consumer;

/**
 * 自定义 {@link org.noear.solon.ai.chat.tool.FunctionTool} 构建上下文。
 *
 * @param agent      当前智能体实体
 * @param toolCodes  已启用的管理端工具 code 列表
 * @param chatConfig 对话配置（可据此构造 ChatModel）
 * @param onStatus   工具执行过程状态回调；可为 null
 * @author 雨夜终端
 */
public record FunctionToolBuildContext(
        AgentAgent agent,
        List<String> toolCodes,
        ChatConfig chatConfig,
        Consumer<String> onStatus
) {
}
