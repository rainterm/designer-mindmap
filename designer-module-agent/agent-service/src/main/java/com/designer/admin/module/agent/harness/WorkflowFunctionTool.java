package com.designer.admin.module.agent.harness;

import com.designer.admin.module.agent.model.entity.workflow.AgentWorkflow;
import com.designer.admin.module.agent.model.vo.workflow.WorkflowRunResult;
import com.designer.admin.module.agent.util.AgentJson;
import com.designer.admin.module.agent.workflow.AgentWorkflowExecutor;
import com.designer.admin.module.agent.workflow.AgentWorkflowStartInputs;
import com.designer.admin.module.agent.workflow.WorkflowRunListener;
import org.noear.solon.ai.chat.ChatModel;
import org.noear.solon.ai.chat.tool.FunctionTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 把绑定的管理工作流包装为 LLM 可调用的函数工具。
 * 模型在 ReAct 循环中决定调用并生成参数（如 SQL），结果作为 Observation 回传后由模型再组织回答。
 *
 * @author 雨夜终端
 */
public class WorkflowFunctionTool implements FunctionTool {

    private static final Logger log = LoggerFactory.getLogger(WorkflowFunctionTool.class);
    private static final int OUTPUT_LIMIT = 6000;

    private final AgentWorkflow workflow;
    private final AgentWorkflowExecutor executor;
    private final ChatModel chatModel;
    private final Consumer<String> onStatus;

    public WorkflowFunctionTool(AgentWorkflow workflow, AgentWorkflowExecutor executor,
                                ChatModel chatModel, Consumer<String> onStatus) {
        this.workflow = workflow;
        this.executor = executor;
        this.chatModel = chatModel;
        this.onStatus = onStatus;
    }

    @Override
    public String name() {
        // 工具名须 [a-zA-Z0-9_] 且以字母开头
        String code = workflow.getCode();
        if (code != null && !code.isBlank()) {
            String sanitized = code.replaceAll("[^a-zA-Z0-9_]", "_");
            if (!sanitized.isEmpty() && Character.isLetter(sanitized.charAt(0))) {
                return "wf_" + sanitized;
            }
        }
        return "wf_" + workflow.getId();
    }

    @Override
    public String title() {
        return workflow.getName() == null || workflow.getName().isBlank()
                ? ("工作流#" + workflow.getId()) : workflow.getName();
    }

    @Override
    public String description() {
        StringBuilder sb = new StringBuilder();
        String desc = workflow.getDescription();
        if (desc != null && !desc.isBlank()) {
            sb.append(desc.trim());
        } else {
            sb.append("执行预定义工作流：").append(title());
        }
        List<Map<String, Object>> nodes = AgentJson.parseMapList(workflow.getNodesJson());
        List<AgentWorkflowStartInputs.Spec> specs = AgentWorkflowStartInputs.parseFromNodes(nodes);
        sb.append("。参数：");
        for (AgentWorkflowStartInputs.Spec spec : specs) {
            sb.append("\n- ").append(spec.key()).append(": ").append(spec.label());
            if (spec.required()) {
                sb.append("（必填）");
            }
        }
        return sb.toString();
    }

    @Override
    public String inputSchema() {
        List<Map<String, Object>> nodes = AgentJson.parseMapList(workflow.getNodesJson());
        List<AgentWorkflowStartInputs.Spec> specs = AgentWorkflowStartInputs.parseFromNodes(nodes);
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"object\",\"properties\":{");
        boolean first = true;
        for (AgentWorkflowStartInputs.Spec spec : specs) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            // spec.key 已被 sanitizeKey 处理为合法标识符，无需转义
            sb.append("\"").append(spec.key()).append("\":{\"type\":\"string\",\"description\":\"")
                    .append(escape(spec.label())).append("\"}");
        }
        sb.append("},\"required\":[");
        boolean firstReq = true;
        for (AgentWorkflowStartInputs.Spec spec : specs) {
            if (!spec.required()) {
                continue;
            }
            if (!firstReq) {
                sb.append(",");
            }
            firstReq = false;
            sb.append("\"").append(spec.key()).append("\"");
        }
        sb.append("]}");
        return sb.toString();
    }

    @Override
    public boolean returnDirect() {
        // 结果回传给模型，由模型再组织最终回答
        return false;
    }

    @Override
    public Type returnType() {
        return String.class;
    }

    @Override
    public Object handle(Map<String, Object> args) throws Throwable {
        WorkflowRunListener listener = new WorkflowRunListener() {
            @Override
            public void onStatus(String status) {
                if (onStatus != null && status != null && !status.isBlank()) {
                    onStatus.accept(status);
                }
            }
        };
        try {
            WorkflowRunResult r = executor.executeForTool(workflow, args, chatModel, listener);
            String out = r.getOutput();
            if (out == null) {
                out = "";
            }
            if (out.length() > OUTPUT_LIMIT) {
                out = out.substring(0, OUTPUT_LIMIT) + "\n…(已截断)";
            }
            return out;
        } catch (Exception e) {
            // 不抛断 ReAct 循环，让模型知道工具失败并据此回答
            log.warn("工作流工具执行失败 workflowId={} code={}", workflow.getId(), workflow.getCode(), e);
            String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            return "工作流执行失败: " + msg;
        }
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
