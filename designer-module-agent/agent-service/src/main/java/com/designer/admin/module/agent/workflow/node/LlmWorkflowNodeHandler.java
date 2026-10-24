package com.designer.admin.module.agent.workflow.node;

import com.designer.admin.module.agent.constant.workflow.WorkflowNodeType;
import com.designer.admin.module.agent.model.dto.workflow.node.LlmNodeConfigDTO;
import com.designer.admin.module.agent.model.vo.workflow.WorkflowRunResult;
import com.designer.admin.module.agent.workflow.AgentWorkflowLlmSupport;
import com.designer.admin.module.agent.workflow.WorkflowCtxKeys;
import org.noear.solon.Utils;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.ai.chat.ChatModel;

import java.util.Map;
import java.util.function.Consumer;

/**
 * LLM 节点：调用模型，可选流式推送回答/思考。
 *
 * @author 雨夜终端
 */
@Component
public class LlmWorkflowNodeHandler implements WorkflowNodeHandler {

    /** 步骤思考文本最大保留字数。 */
    private static final int STEP_TEXT_LIMIT = 8000;

    /** LLM 调用与流式推送支持。 */
    @Inject
    private AgentWorkflowLlmSupport llmSupport;

    /**
     * {@inheritDoc}
     * <p>固定返回 {@link WorkflowNodeType#LLM}。
     */
    @Override
    public WorkflowNodeType type() {
        return WorkflowNodeType.LLM;
    }

    /**
     * {@inheritDoc}
     * <p>提取模型、提示词、流式/思考开关及技能/工具/MCP 等试跑参数。
     *
     * @param configJson 节点配置 JSON
     * @param vars       当前工作流变量表（本节点描述未做变量替换）
     * @return 试跑 step.params 字段
     */
    @Override
    public Map<String, Object> describeParams(String configJson, Map<String, String> vars) {
        LlmNodeConfigDTO cfg = WorkflowNodeConfigs.parse(configJson, LlmNodeConfigDTO.class);
        Map<String, Object> fallback = WorkflowNodeConfigs.rawConfigFallback(configJson, cfg);
        if (fallback != null) {
            return fallback;
        }
        normalizeForDescribe(cfg);
        return WorkflowNodeConfigs.toParamMap(cfg);
    }

    /**
     * 调用大模型生成文本，并按配置推送回答/思考增量、回写步骤详情。
     *
     * @param ctx 节点执行上下文
     * @return 模型输出全文
     * @throws Exception LLM 调用失败时抛出
     */
    @Override
    public String execute(WorkflowNodeContext ctx) throws Exception {
        LlmNodeConfigDTO cfg = WorkflowNodeConfigs.parse(ctx.config(), LlmNodeConfigDTO.class);
        if (cfg == null) {
            cfg = new LlmNodeConfigDTO();
        }
        ChatModel fallback = ctx.resolveChatModel(WorkflowCtxKeys.CHAT_MODEL);
        boolean emitAnswer = Boolean.TRUE.equals(cfg.getEmitAnswer());
        boolean stream = cfg.getStream() == null || Boolean.TRUE.equals(cfg.getStream());
        boolean emitThinking = Boolean.TRUE.equals(cfg.getEmitThinking());

        Consumer<String> onToken = null;
        if (emitAnswer && stream) {
            onToken = ctx::emitAnswerDelta;
        }
        Consumer<String> onThinking = null;
        StringBuilder thinkingOut = emitThinking ? new StringBuilder() : null;
        if (emitThinking) {
            onThinking = ctx::emitThinkingDelta;
        }
        boolean needStream = onToken != null || onThinking != null;
        String text = llmSupport.run(cfg, ctx.vars(), fallback,
                needStream ? onToken : null,
                needStream ? onThinking : null,
                needStream ? thinkingOut : null);
        if (emitAnswer && !stream) {
            ctx.emitAnswerDelta(text);
        }
        WorkflowRunResult.Step step = ctx.step();
        if (thinkingOut != null && !thinkingOut.isEmpty()) {
            step.setThinking(truncateText(thinkingOut.toString()));
        }
        if (step.getDetail() == null || step.getDetail().isBlank()) {
            StringBuilder detail = new StringBuilder();
            if (emitAnswer) {
                detail.append(stream ? "已流式输出回答" : "已整段输出回答");
            } else {
                detail.append("内部结果（仅供下游）");
            }
            if (emitThinking) {
                detail.append(thinkingOut != null && !thinkingOut.isEmpty() ? " · 含思考过程" : " · 无思考内容");
            }
            step.setDetail(detail.toString());
        }
        return text;
    }

    /**
     * 试跑面板用：补齐布尔默认值与空 userPrompt。
     *
     * @param cfg LLM 配置
     */
    private static void normalizeForDescribe(LlmNodeConfigDTO cfg) {
        if (cfg.getEmitAnswer() == null) {
            cfg.setEmitAnswer(false);
        }
        if (cfg.getStream() == null) {
            cfg.setStream(true);
        }
        if (cfg.getEmitThinking() == null) {
            cfg.setEmitThinking(false);
        }
        if (Utils.isBlank(cfg.getSystemPrompt())) {
            cfg.setSystemPrompt("");
        }
        if (Utils.isBlank(cfg.getUserPrompt())) {
            cfg.setUserPrompt("请根据上下文给出回答。\n\n上下文：\n{output}");
        }
    }

    /**
     * 截断过长文本，避免步骤元数据过大。
     *
     * @param s 原始文本
     * @return 截断后文本；null 时返回空串
     */
    private static String truncateText(String s) {
        if (s == null) {
            return "";
        }
        if (s.length() <= STEP_TEXT_LIMIT) {
            return s;
        }
        return s.substring(0, STEP_TEXT_LIMIT) + "…(截断,共" + s.length() + "字)";
    }
}
