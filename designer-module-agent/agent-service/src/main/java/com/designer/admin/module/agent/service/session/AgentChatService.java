package com.designer.admin.module.agent.service.session;

import com.designer.admin.module.agent.model.dto.session.ChatAttachmentDTO;
import com.designer.admin.module.agent.model.dto.session.ChatStreamResult;
import com.designer.admin.module.agent.model.dto.session.ChatToolCallDTO;
import com.designer.admin.module.agent.model.entity.agent.AgentAgent;
import com.designer.admin.module.agent.model.entity.session.AgentSession;
import java.util.List;
import java.util.function.Consumer;

/**
 * 智能体调试对话
 *
 * @author 雨夜终端
 */
public interface AgentChatService {

    /**
     * 同步对话（无附件）。
     *
     * @param session     会话
     * @param agent       智能体（可含草稿覆盖后的运行配置）
     * @param userContent 用户输入
     * @return 助手完整回复文本
     */
    String chat(AgentSession session, AgentAgent agent, String userContent);

    /**
     * 同步对话（可带附件）。
     *
     * @param session     会话
     * @param agent       智能体（可含草稿覆盖后的运行配置）
     * @param userContent 用户输入
     * @param attachments 附件列表，可空
     * @return 助手完整回复文本
     */
    String chat(AgentSession session, AgentAgent agent, String userContent, List<ChatAttachmentDTO> attachments);

    /**
     * 流式对话：文本增量 {@code onDelta}，工具过程 {@code onTool}；返回完整回复与工具轨迹。
     *
     * @param session     会话
     * @param agent       智能体
     * @param userContent 用户输入
     * @param attachments 附件列表，可空
     * @param onDelta     正文增量回调，可空
     * @param onTool      工具调用回调，可空
     * @return 完整回复与工具轨迹
     */
    ChatStreamResult chatStream(AgentSession session, AgentAgent agent, String userContent,
                                List<ChatAttachmentDTO> attachments, Consumer<String> onDelta,
                                Consumer<ChatToolCallDTO> onTool);

    /**
     * 流式对话：在文本/工具回调基础上增加状态 {@code onStatus}。
     *
     * @param session     会话
     * @param agent       智能体
     * @param userContent 用户输入
     * @param attachments 附件列表，可空
     * @param onDelta     正文增量回调，可空
     * @param onTool      工具调用回调，可空
     * @param onStatus    状态文案回调，可空
     * @return 完整回复与工具轨迹
     */
    ChatStreamResult chatStream(AgentSession session, AgentAgent agent, String userContent,
                                List<ChatAttachmentDTO> attachments, Consumer<String> onDelta,
                                Consumer<ChatToolCallDTO> onTool, Consumer<String> onStatus);

    /**
     * 流式对话：文本增量、工具过程、状态、思考增量均可选回调；返回完整回复与工具轨迹。
     *
     * @param session     会话
     * @param agent       智能体
     * @param userContent 用户输入
     * @param attachments 附件列表，可空
     * @param onDelta     正文增量回调，可空
     * @param onTool      工具调用回调，可空
     * @param onStatus    状态文案回调，可空
     * @param onThinking  思考增量回调，可空
     * @return 完整回复与工具轨迹
     */
    ChatStreamResult chatStream(AgentSession session, AgentAgent agent, String userContent,
                                List<ChatAttachmentDTO> attachments, Consumer<String> onDelta,
                                Consumer<ChatToolCallDTO> onTool, Consumer<String> onStatus,
                                Consumer<String> onThinking);

}
