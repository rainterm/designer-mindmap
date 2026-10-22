package com.designer.admin.module.agent.workflow.node;

import com.designer.admin.module.agent.constant.workflow.WorkflowNodeType;
import com.designer.admin.module.agent.model.vo.workflow.WorkflowRunResult;
import com.designer.admin.module.agent.util.AgentJson;
import com.designer.admin.module.agent.workflow.AgentWorkflowExecutor;
import com.designer.admin.module.agent.workflow.AgentWorkflowLlmSupport;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.ai.chat.ChatModel;

import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * LLM 节点：调用模型，可选流式推送回答/思考。
 *
 * @author 雨夜终端
 */
@Component
public class LlmWorkflowNodeHandler implements WorkflowNodeHandler {

    private static final int STEP_TEXT_LIMIT = 8000;

    @Inject
    private AgentWorkflowLlmSupport llmSupport;

    @Override
    public WorkflowNodeType type() {
        return WorkflowNodeType.LLM;
    }

    @Override
    public String execute(WorkflowNodeContext ctx) throws Exception {
        Map<String, Object> cfg = parseConfig(ctx.config());
        ChatModel fallback = ctx.resolveChatModel(AgentWorkflowExecutor.CTX_CHAT_MODEL);
        boolean emitAnswer = boolVal(cfg.get("emitAnswer"), false);
        boolean stream = boolVal(cfg.get("stream"), true);
        boolean emitThinking = boolVal(cfg.get("emitThinking"), false);

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

    private static Map<String, Object> parseConfig(String config) {
        if (config == null || config.isBlank()) {
            return Map.of();
        }
        return AgentJson.parseMap(config.trim());
    }

    private static boolean boolVal(Object o, boolean def) {
        if (o == null) {
            return def;
        }
        if (o instanceof Boolean b) {
            return b;
        }
        String s = String.valueOf(o).trim().toLowerCase(Locale.ROOT);
        if (s.isEmpty()) {
            return def;
        }
        return "1".equals(s) || "true".equals(s) || "yes".equals(s) || "y".equals(s);
    }

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
