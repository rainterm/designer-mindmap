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

    /** Solon Flow 图执行上下文。 */
    private final FlowContext flowContext;
    /** 节点配置 JSON 字符串。 */
    private final String config;
    /** 当前工作流变量表。 */
    private final Map<String, String> vars;
    /** 当前步骤运行结果元数据。 */
    private final WorkflowRunResult.Step step;
    /** 完整回答按块推送回调（输出节点等）。 */
    private final Consumer<String> answerChunkEmitter;
    /** 流式回答增量推送回调。 */
    private final Consumer<String> answerDeltaEmitter;
    /** 思考过程增量推送回调。 */
    private final Consumer<String> thinkingDeltaEmitter;
    /** 图上下文无模型时的 ChatModel 回退供应器。 */
    private final Supplier<ChatModel> chatModelFallback;

    /**
     * 构造精简上下文（仅回答分块推送）。
     *
     * @param flowContext         图执行上下文
     * @param config              节点配置 JSON
     * @param vars                工作流变量表
     * @param step                步骤元数据
     * @param answerChunkEmitter  回答分块推送回调，可为 null
     */
    public WorkflowNodeContext(FlowContext flowContext,
                               String config,
                               Map<String, String> vars,
                               WorkflowRunResult.Step step,
                               Consumer<String> answerChunkEmitter) {
        this(flowContext, config, vars, step, answerChunkEmitter, null, null, null);
    }

    /**
     * 构造完整节点执行上下文。
     *
     * @param flowContext           图执行上下文
     * @param config                节点配置 JSON
     * @param vars                  工作流变量表
     * @param step                  步骤元数据
     * @param answerChunkEmitter    回答分块推送回调，可为 null
     * @param answerDeltaEmitter    流式回答增量回调，可为 null
     * @param thinkingDeltaEmitter  思考增量回调，可为 null
     * @param chatModelFallback     ChatModel 回退供应器，可为 null
     */
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

    /**
     * 获取图执行上下文。
     *
     * @return Solon Flow 上下文
     */
    public FlowContext flowContext() {
        return flowContext;
    }

    /**
     * 获取节点配置 JSON。
     *
     * @return 配置字符串，可能为 null
     */
    public String config() {
        return config;
    }

    /**
     * 获取工作流变量表。
     *
     * @return 变量名到值的映射
     */
    public Map<String, String> vars() {
        return vars;
    }

    /**
     * 获取当前步骤元数据。
     *
     * @return 运行步骤对象
     */
    public WorkflowRunResult.Step step() {
        return step;
    }

    /**
     * 将完整文本按块推送给回答流（输出节点等）。
     *
     * @param text 待推送文本；为空或未配置回调时忽略
     */
    public void emitAnswerChunked(String text) {
        if (answerChunkEmitter != null && text != null && !text.isEmpty()) {
            answerChunkEmitter.accept(text);
        }
    }

    /**
     * 推送流式回答增量。
     *
     * @param delta 增量文本；为空或未配置回调时忽略
     */
    public void emitAnswerDelta(String delta) {
        if (answerDeltaEmitter != null && delta != null && !delta.isEmpty()) {
            answerDeltaEmitter.accept(delta);
        }
    }

    /**
     * 推送思考过程增量。
     *
     * @param delta 增量文本；为空或未配置回调时忽略
     */
    public void emitThinkingDelta(String delta) {
        if (thinkingDeltaEmitter != null && delta != null && !delta.isEmpty()) {
            thinkingDeltaEmitter.accept(delta);
        }
    }

    /**
     * 解析 ChatModel：优先图上下文中的模型，否则回退默认智能体模型。
     *
     * @param ctxChatModelKey 图上下文中存放 ChatModel 的键
     * @return 可用 ChatModel；均不可用时返回 null
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
