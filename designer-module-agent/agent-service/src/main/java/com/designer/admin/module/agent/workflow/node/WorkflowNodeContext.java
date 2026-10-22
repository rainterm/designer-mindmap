package com.designer.admin.module.agent.workflow.node;

import com.designer.admin.module.agent.model.vo.workflow.WorkflowRunResult;
import org.noear.solon.ai.chat.ChatModel;
import org.noear.solon.flow.FlowContext;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 节点执行上下文：图上下文 + 配置 + 变量 + 步骤元数据 + 回答/思考推送回调。
 *
 * @author 雨夜终端
 */
public final class WorkflowNodeContext {

    private final FlowContext flowContext;
    private final String config;
    private final Map<String, String> vars;
    private final WorkflowRunResult.Step step;
    private final Consumer<String> answerChunkEmitter;
    private final Consumer<String> answerDeltaEmitter;
    private final Consumer<String> thinkingDeltaEmitter;
    private final Supplier<ChatModel> chatModelFallback;

    public WorkflowNodeContext(FlowContext flowContext,
                               String config,
                               Map<String, String> vars,
                               WorkflowRunResult.Step step,
                               Consumer<String> answerChunkEmitter) {
        this(flowContext, config, vars, step, answerChunkEmitter, null, null, null);
    }

    public WorkflowNodeContext(FlowContext flowContext,
                               String config,
                               Map<String, String> vars,
                               WorkflowRunResult.Step step,
                               Consumer<String> answerChunkEmitter,
                               Consumer<String> answerDeltaEmitter,
                               Consumer<String> thinkingDeltaEmitter,
                               Supplier<ChatModel> chatModelFallback) {
        this.flowContext = flowContext;
        this.config = config;
        this.vars = vars;
        this.step = step;
        this.answerChunkEmitter = answerChunkEmitter;
        this.answerDeltaEmitter = answerDeltaEmitter;
        this.thinkingDeltaEmitter = thinkingDeltaEmitter;
        this.chatModelFallback = chatModelFallback;
    }

    public FlowContext flowContext() {
        return flowContext;
    }

    public String config() {
        return config;
    }

    public Map<String, String> vars() {
        return vars;
    }

    public WorkflowRunResult.Step step() {
        return step;
    }

    /** 将完整文本按块推送给回答流（输出节点等）。 */
    public void emitAnswerChunked(String text) {
        if (answerChunkEmitter != null && text != null && !text.isEmpty()) {
            answerChunkEmitter.accept(text);
        }
    }

    /** 流式回答增量。 */
    public void emitAnswerDelta(String delta) {
        if (answerDeltaEmitter != null && delta != null && !delta.isEmpty()) {
            answerDeltaEmitter.accept(delta);
        }
    }

    /** 思考过程增量。 */
    public void emitThinkingDelta(String delta) {
        if (thinkingDeltaEmitter != null && delta != null && !delta.isEmpty()) {
            thinkingDeltaEmitter.accept(delta);
        }
    }

    /**
     * 解析 ChatModel：优先图上下文中的模型，否则回退默认智能体模型。
     */
    public ChatModel resolveChatModel(String ctxChatModelKey) {
        if (flowContext != null && ctxChatModelKey != null) {
            ChatModel fromCtx = flowContext.getAs(ctxChatModelKey);
            if (fromCtx != null) {
                return fromCtx;
            }
        }
        return chatModelFallback == null ? null : chatModelFallback.get();
    }
}
