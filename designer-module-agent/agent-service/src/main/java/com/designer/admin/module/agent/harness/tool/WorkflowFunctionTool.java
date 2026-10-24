package com.designer.admin.module.agent.harness.tool;

import com.designer.admin.module.agent.model.entity.workflow.AgentWorkflow;
import com.designer.admin.module.agent.model.vo.workflow.WorkflowRunResult;
import com.designer.admin.module.agent.util.AgentJsonUtil;
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

    /** 日志 */
    private static final Logger log = LoggerFactory.getLogger(WorkflowFunctionTool.class);
    /** 工具输出截断上限（字符数） */
    private static final int OUTPUT_LIMIT = 6000;

    /** 绑定的工作流实体 */
    private final AgentWorkflow workflow;
    /** 工作流执行器 */
    private final AgentWorkflowExecutor executor;
    /** 借用的对话模型（节点 LLM 用） */
    private final ChatModel chatModel;
    /** 过程状态回调；可为 null */
    private final Consumer<String> onStatus;

    /**
     * @param workflow  绑定的工作流
     * @param executor  执行器
     * @param chatModel 可选 ChatModel
     * @param onStatus  过程状态回调；可为 null
     */
    public WorkflowFunctionTool(AgentWorkflow workflow, AgentWorkflowExecutor executor,
                                ChatModel chatModel, Consumer<String> onStatus) {
        this.workflow = workflow;
        this.executor = executor;
        this.chatModel = chatModel;
        this.onStatus = onStatus;
    }

    /**
     * 工具名：{@code wf_} + 安全 code，或 {@code wf_} + 工作流 ID。
     *
     * @return 符合 [a-zA-Z0-9_] 且字母开头的工具名
     */
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

    /**
     * 工具展示标题（工作流名称）。
     *
     * @return 展示名
     */
    @Override
    public String title() {
        return workflow.getName() == null || workflow.getName().isBlank()
                ? ("工作流#" + workflow.getId()) : workflow.getName();
    }

    /**
     * 工具说明：工作流描述 + 开始节点参数列表。
     *
     * @return 供模型阅读的描述文案
     */
    @Override
    public String description() {
        StringBuilder sb = new StringBuilder();
        String desc = workflow.getDescription();
        if (desc != null && !desc.isBlank()) {
            sb.append(desc.trim());
        } else {
            sb.append("执行预定义工作流：").append(title());
        }
        List<Map<String, Object>> nodes = AgentJsonUtil.parseMapList(workflow.getNodesJson());
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

    /**
     * 按开始节点输入规格生成 JSON Schema。
     *
     * @return inputSchema JSON 字符串
     */
    @Override
    public String inputSchema() {
        List<Map<String, Object>> nodes = AgentJsonUtil.parseMapList(workflow.getNodesJson());
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

    /**
     * 结果回传给模型，由模型再组织最终回答。
     *
     * @return 恒为 false
     */
    @Override
    public boolean returnDirect() {
        // 结果回传给模型，由模型再组织最终回答
        return false;
    }

    /**
     * 工具返回值类型。
     *
     * @return {@link String}
     */
    @Override
    public Type returnType() {
        return String.class;
    }

    /**
     * 同步执行工作流，将输出截断后作为 Observation 返回；失败不抛断 ReAct。
     *
     * @param args 模型生成的入参
     * @return 工作流输出文本或失败说明
     */
    @Override
    public Object handle(Map<String, Object> args) throws Throwable {
        WorkflowRunListener listener = new WorkflowRunListener() {
            /** 转发工作流过程状态到对话侧回调。 */
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

    /**
     * JSON 字符串字面量转义。
     *
     * @param s 原始文本
     * @return 转义后文本
     */
    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
