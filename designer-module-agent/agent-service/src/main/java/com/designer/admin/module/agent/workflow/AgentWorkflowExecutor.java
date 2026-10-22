package com.designer.admin.module.agent.workflow;

import com.designer.admin.module.agent.util.AgentTexts;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.module.agent.mapper.workflow.AgentWorkflowMapper;
import com.designer.admin.module.agent.model.entity.agent.AgentAgent;
import com.designer.admin.module.agent.model.entity.workflow.AgentWorkflow;
import com.designer.admin.module.agent.model.vo.workflow.WorkflowRunResult;
import com.designer.admin.module.agent.util.AgentJson;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.ai.chat.ChatModel;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.FlowEngine;
import org.noear.solon.flow.Graph;
import org.noear.solon.flow.Node;
import org.noear.solon.flow.NodeType;
import org.noear.solon.flow.TaskComponent;
import org.noear.solon.flow.intercept.FlowInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import com.designer.admin.common.constant.SystemConsts;
import com.designer.admin.module.agent.constant.AgentConsts;
import com.designer.admin.module.agent.constant.workflow.WorkflowNodeType;
import com.designer.admin.module.agent.workflow.activity.WorkflowHttpActivity;
import com.designer.admin.module.agent.workflow.node.WorkflowNodeContext;
import com.designer.admin.module.agent.workflow.node.WorkflowNodeHandler;
import com.designer.admin.module.agent.workflow.node.WorkflowNodeHandlerRegistry;
import com.designer.admin.module.agent.workflow.support.WorkflowCodeEngineSupport;
import com.designer.admin.module.agent.workflow.support.WorkflowVarSupport;
/**
 * 基于 Solon Flow 执行管理端编排的工作流图。
 *
 * @author 雨夜终端
 */
@Component
public class AgentWorkflowExecutor {

    private static final Logger log = LoggerFactory.getLogger(AgentWorkflowExecutor.class);

    public static final String CTX_INPUT = AgentConsts.CTX_INPUT;
    public static final String CTX_OUTPUT = AgentConsts.CTX_OUTPUT;
    public static final String CTX_BRANCH = "branch";
    public static final String CTX_STEPS = "_steps";
    public static final String CTX_CHAT_MODEL = "_chatModel";
    /** 开始节点解析后的全部入参 */
    public static final String CTX_VARS = "_vars";
    /** 对外回答内容累积 */
    public static final String CTX_ANSWER = "_answer";
    public static final String CTX_LISTENER = "_listener";

    @Db
    private AgentWorkflowMapper workflowMapper;
    @Inject
    private WorkflowCodeEngineSupport codeEngineSupport;
    @Inject
    private WorkflowNodeHandlerRegistry nodeHandlerRegistry;
    @Inject
    private AgentWorkflowChatModelSupport chatModelSupport;

    public WorkflowRunResult run(Long workflowId, Map<String, Object> params, Long agentId) {
        return run(workflowId, params, agentId, null, true);
    }

    public WorkflowRunResult run(Long workflowId, Map<String, Object> params, Long agentId,
                                 WorkflowRunListener listener) {
        return run(workflowId, params, agentId, listener, true);
    }

    /**
     * 执行工作流。
     *
     * @param useDraft true=管理端试跑（优先草稿图）；false=开放接口（仅已发布正式图）
     */
    public WorkflowRunResult run(Long workflowId, Map<String, Object> params, Long agentId,
                                 WorkflowRunListener listener, boolean useDraft) {
        AgentWorkflow wf = workflowMapper.selectById(workflowId);
        if (wf == null) {
            throw new BizException("工作流不存在");
        }
        if (wf.getStatus() != null && wf.getStatus() != SystemConsts.STATUS_ENABLE) {
            throw new BizException("工作流已停用");
        }
        if (useDraft) {
            // 试跑面向草稿：优先草稿图，验证未发布的改动；草稿为空（旧数据）退回正式图
            wf.setNodesJson(AgentTexts.notBlank(wf.getDraftNodesJson()) ? wf.getDraftNodesJson() : wf.getNodesJson());
            wf.setEdgesJson(AgentTexts.notBlank(wf.getDraftEdgesJson()) ? wf.getDraftEdgesJson() : wf.getEdgesJson());
        } else if (!AgentTexts.notBlank(wf.getNodesJson())) {
            throw new BizException("工作流尚未发布");
        }
        ChatModel chatModel = resolveChatModel(agentId);
        return execute(wf, params, chatModel, listener);
    }

    /** 供智能体对话：用户消息写入开始节点第一个参数，按绑定工作流依次执行。 */
    public String runBound(AgentAgent agent, String userMessage) {
        return runBound(agent, userMessage, null);
    }

    public String runBound(AgentAgent agent, String userMessage, WorkflowRunListener listener) {
        List<Long> ids = AgentJson.parseLongList(agent.getWorkflowIdsJson());
        if (ids.isEmpty()) {
            return null;
        }
        List<AgentWorkflow> list = workflowMapper.selectList(new LambdaQueryWrapper<AgentWorkflow>()
                        .in(AgentWorkflow::getId, ids)
                        .eq(AgentWorkflow::getStatus, SystemConsts.STATUS_ENABLE)
                        .orderByAsc(AgentWorkflow::getId))
                // 只执行已发布的工作流：正式图为空（从未发布）跳过
                .stream()
                .filter(wf -> AgentTexts.notBlank(wf.getNodesJson()))
                .toList();
        if (list == null || list.isEmpty()) {
            return null;
        }
        ChatModel chatModel = resolveChatModel(agent.getId());
        String current = userMessage == null ? "" : userMessage;
        String lastOut = null;
        for (AgentWorkflow wf : list) {
            List<Map<String, Object>> nodes = AgentJson.parseMapList(wf.getNodesJson());
            var specs = AgentWorkflowStartInputs.parseFromNodes(nodes);
            Map<String, Object> params = new LinkedHashMap<>();
            if (!specs.isEmpty()) {
                params.put(specs.get(0).key(), current);
            }
            WorkflowRunResult r = execute(wf, params, chatModel, listener);
            lastOut = r.getOutput();
            if (lastOut != null && !lastOut.isBlank()) {
                current = lastOut;
            }
        }
        return lastOut;
    }

    /** 查询指定 id 列表中启用且已发布的管理工作流（按 id 升序）。 */
    public List<AgentWorkflow> listBoundWorkflows(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<AgentWorkflow> list = workflowMapper.selectList(new LambdaQueryWrapper<AgentWorkflow>()
                        .in(AgentWorkflow::getId, ids)
                        .eq(AgentWorkflow::getStatus, SystemConsts.STATUS_ENABLE)
                        .orderByAsc(AgentWorkflow::getId));
        // 只暴露已发布的工作流：正式图为空（从未发布）不作为可调用工具
        return list == null ? List.of()
                : list.stream().filter(wf -> AgentTexts.notBlank(wf.getNodesJson())).toList();
    }

    /** 供工作流工具回调：按 LLM 提供的参数执行工作流。 */
    public WorkflowRunResult executeForTool(AgentWorkflow wf, Map<String, Object> params,
                                            ChatModel chatModel, WorkflowRunListener listener) {
        return execute(wf, params, chatModel, listener);
    }

    private WorkflowRunResult execute(AgentWorkflow wf, Map<String, Object> params, ChatModel chatModel,
                                      WorkflowRunListener listener) {
        List<Map<String, Object>> nodes = AgentJson.parseMapList(wf.getNodesJson());
        List<Map<String, Object>> edges = AgentJson.parseMapList(wf.getEdgesJson());
        List<WorkflowRunResult.Step> steps = new CopyOnWriteArrayList<>();
        WorkflowRunListener bus = listener == null ? new WorkflowRunListener() {
        } : listener;

        var specs = AgentWorkflowStartInputs.parseFromNodes(nodes);
        Map<String, String> vars = AgentWorkflowStartInputs.resolve(specs, params);
        String seed = vars.isEmpty() ? "" : vars.values().iterator().next();

        AgentWorkflowNodeTasks tasks = new AgentWorkflowNodeTasks() {
            @Override
            public TaskComponent activityTask(String key, String type, String name, String config, String outputVar) {
                return (ctx, node) -> handleActivity(ctx, key, type, name, config, outputVar, steps);
            }

            @Override
            public TaskComponent conditionTask(String key, String name, String config) {
                return (ctx, node) -> handleCondition(ctx, key, name, config, steps, bus);
            }
        };

        Graph graph = AgentWorkflowGraphBuilder.build(wf.getId(), wf.getName(), nodes, edges, tasks);
        FlowContext context = FlowContext.of("run-" + wf.getId() + "-" + System.currentTimeMillis());
        context.put(CTX_INPUT, seed);
        context.put(CTX_OUTPUT, "");
        context.put(CTX_BRANCH, "yes");
        context.put(CTX_STEPS, steps);
        context.put(CTX_VARS, new LinkedHashMap<>(vars));
        context.put(CTX_ANSWER, new StringBuilder());
        context.put(CTX_LISTENER, bus);
        for (Map.Entry<String, String> e : vars.entrySet()) {
            context.put(e.getKey(), e.getValue());
        }
        if (chatModel != null) {
            context.put(CTX_CHAT_MODEL, chatModel);
        }

        bus.onStatus("正在执行工作流…");
        try {
            FlowEngine engine = FlowEngine.newInstance();
            engine.addInterceptor(new FlowInterceptor() {
                @Override
                public void onNodeStart(FlowContext ctx, Node node) {
                    if (node != null && node.getType() == NodeType.START) {
                        handleStart(ctx, node.getId(), node.getTitle(), node.getMetaAsString("config"), steps);
                    }
                }

                @Override
                public void onNodeEnd(FlowContext ctx, Node node) {
                    if (node != null && node.getType() == NodeType.END) {
                        handleEnd(ctx, node.getId(), node.getTitle(), node.getMetaAsString("config"), steps);
                    }
                }
            });
            engine.eval(graph, context);
        } catch (BizException e) {
            bus.onError(e.getMessage());
            throw e;
        } catch (Throwable e) {
            if (e instanceof Error err) {
                throw err;
            }
            log.error("工作流执行失败 id={}", wf.getId(), e);
            String msg = "工作流执行失败: " + rootMessage(e);
            bus.onError(msg);
            throw new BizException(msg, e);
        }

        WorkflowRunResult result = new WorkflowRunResult();
        result.setWorkflowId(wf.getId());
        result.setWorkflowName(wf.getName());
        String answer = answerOf(context);
        if (answer.isBlank()) {
            Object out = context.get(CTX_OUTPUT);
            answer = out == null ? "" : String.valueOf(out);
        }
        result.setOutput(answer);
        result.setSteps(new ArrayList<>(steps));
        return result;
    }

    private static final int STEP_TEXT_LIMIT = 8000;

    private void handleStart(FlowContext ctx, String key, String name, String config,
                             List<WorkflowRunResult.Step> steps) {
        long t0 = System.currentTimeMillis();
        WorkflowRunListener bus = listenerOf(ctx);
        Map<String, String> inputVars = startVarsOf(ctx);
        WorkflowRunResult.Step step = new WorkflowRunResult.Step();
        step.setNodeKey(key);
        step.setNodeType(WorkflowNodeType.START.getCode());
        step.setNodeName(name == null || name.isBlank() ? "开始" : name);
        step.setInput(truncateMap(inputVars));
        Map<String, Object> params = new LinkedHashMap<>();
        List<AgentWorkflowStartInputs.Spec> specs = AgentWorkflowStartInputs.parseConfig(config);
        List<Map<String, Object>> inputsMeta = new ArrayList<>();
        for (AgentWorkflowStartInputs.Spec s : specs) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("key", s.key());
            m.put("label", s.label());
            m.put("required", s.required());
            if (s.defaultValue() != null && !s.defaultValue().isBlank()) {
                m.put("defaultValue", s.defaultValue());
            }
            inputsMeta.add(m);
        }
        params.put("inputs", inputsMeta);
        step.setParams(params);
        step.setOutput(truncateText(AgentJson.toJson(inputVars)));
        step.setDetail("已注入 " + inputVars.size() + " 个输入变量");
        step.setDurationMs(Math.max(0, System.currentTimeMillis() - t0));
        steps.add(step);
        bus.onStatus("执行节点：" + step.getNodeName());
        bus.onStep(step);
    }

    private void handleEnd(FlowContext ctx, String key, String name, String config,
                           List<WorkflowRunResult.Step> steps) {
        long t0 = System.currentTimeMillis();
        WorkflowRunListener bus = listenerOf(ctx);
        Map<String, String> inputVars = varsOf(ctx);
        WorkflowRunResult.Step step = new WorkflowRunResult.Step();
        step.setNodeKey(key);
        step.setNodeType(WorkflowNodeType.END.getCode());
        step.setNodeName(name == null || name.isBlank() ? "结束" : name);
        step.setInput(truncateMap(inputVars));
        Map<String, Object> params = new LinkedHashMap<>();
        String pipelineOut = str(ctx.get(CTX_OUTPUT));
        String answer = answerOf(ctx);
        params.put("pipelineOutput", truncateText(pipelineOut));
        if (!answer.isBlank()) {
            params.put("answerLength", answer.length());
        }
        if (config != null && !config.isBlank()) {
            params.put("config", truncateText(config.trim()));
        }
        step.setParams(params);
        String out = !answer.isBlank() ? answer : pipelineOut;
        step.setOutput(truncateText(out));
        step.setDetail(out.isBlank() ? "工作流结束（无输出）" : "工作流结束");
        step.setDurationMs(Math.max(0, System.currentTimeMillis() - t0));
        steps.add(step);
        bus.onStatus("执行节点：" + step.getNodeName());
        bus.onStep(step);
    }

    private void handleActivity(FlowContext ctx, String key, String type, String name,
                                String config, String outputVar, List<WorkflowRunResult.Step> steps) throws Throwable {
        String t = type == null ? "" : type.toLowerCase(Locale.ROOT);
        WorkflowRunListener bus = listenerOf(ctx);
        WorkflowRunResult.Step step = new WorkflowRunResult.Step();
        step.setNodeKey(key);
        step.setNodeType(t);
        step.setNodeName(name);
        Map<String, String> inputVars = varsOf(ctx);
        step.setInput(truncateMap(inputVars));
        step.setParams(buildStepParams(t, config, outputVar, inputVars));
        bus.onStatus("执行节点：" + (name == null || name.isBlank() ? key : name));
        long t0 = System.currentTimeMillis();
        try {
            WorkflowNodeType nodeType = WorkflowNodeType.fromCode(t);
            String text;
            if (nodeType == null || nodeType == WorkflowNodeType.START
                    || nodeType == WorkflowNodeType.END
                    || nodeType == WorkflowNodeType.CONDITION) {
                step.setDetail("不支持的节点类型: " + t);
                step.setOk(false);
                text = null;
            } else {
                WorkflowNodeHandler handler = nodeHandlerRegistry.get(nodeType);
                if (handler == null) {
                    step.setDetail("未注册节点处理器: " + t);
                    step.setOk(false);
                    text = null;
                } else {
                    WorkflowNodeContext nodeCtx = new WorkflowNodeContext(
                            ctx, config, varsOf(ctx), step,
                            out -> emitAnswerChunked(ctx, out),
                            delta -> emitAnswerDelta(ctx, delta),
                            delta -> listenerOf(ctx).onThinkingDelta(delta),
                            chatModelSupport::resolveDefaultChatModel);
                    text = handler.execute(nodeCtx);
                }
            }
            if (text != null) {
                writeNodeOutput(ctx, WorkflowNodeType.OUTPUT.eq(t) ? "" : outputVar, text, step);
            }
        } catch (Exception e) {
            step.setOk(false);
            step.setDetail(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            step.setDurationMs(Math.max(0, System.currentTimeMillis() - t0));
            steps.add(step);
            bus.onStep(step);
            throw e;
        }
        step.setDurationMs(Math.max(0, System.currentTimeMillis() - t0));
        steps.add(step);
        bus.onStep(step);
    }

    private void writeNodeOutput(FlowContext ctx, String outputVar, String text, WorkflowRunResult.Step step) {
        String t = text == null ? "" : text;
        ctx.put(CTX_OUTPUT, t);
        String var = outputVar == null ? "" : outputVar.trim();
        if (!var.isBlank() && var.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            putVar(ctx, var, t);
        }
        step.setOutput(truncateText(t));
    }

    private Map<String, Object> buildStepParams(String type, String config, String outputVar,
                                                Map<String, String> vars) {
        Map<String, Object> params = new LinkedHashMap<>();
        if (outputVar != null && !outputVar.isBlank()) {
            params.put("outputVar", outputVar.trim());
        }
        Map<String, Object> cfg = parseLlmConfig(config == null ? "" : config);
        if (cfg.isEmpty() && config != null && !config.isBlank()) {
            params.put("config", config.trim());
            return resolveParamsVars(params, vars);
        }
        WorkflowNodeType nodeType = WorkflowNodeType.fromCode(type);
        if (nodeType == null || nodeType == WorkflowNodeType.START
                || nodeType == WorkflowNodeType.END || nodeType == WorkflowNodeType.CONDITION) {
            if (!cfg.isEmpty()) {
                params.put("config", cfg);
            } else if (config != null && !config.isBlank()) {
                params.put("config", config);
            }
        } else {
            switch (nodeType) {
                case LLM:
                    params.put("providerId", cfg.get("providerId"));
                    params.put("modelCode", cfg.get("modelCode"));
                    params.put("temperature", cfg.get("temperature"));
                    params.put("topP", cfg.get("topP"));
                    params.put("maxTokens", cfg.get("maxTokens"));
                    params.put("emitAnswer", boolVal(cfg.get("emitAnswer"), false));
                    params.put("stream", boolVal(cfg.get("stream"), true));
                    params.put("emitThinking", boolVal(cfg.get("emitThinking"), false));
                    params.put("skillCodes", cfg.get("skillCodes"));
                    params.put("toolCodes", cfg.get("toolCodes"));
                    params.put("mcpCodes", cfg.get("mcpCodes"));
                    String systemTpl = str(cfg.get("systemPrompt"));
                    String userTpl = str(cfg.get("userPrompt"));
                    if (userTpl.isBlank()) {
                        userTpl = "请根据上下文给出回答。\n\n上下文：\n{output}";
                    }
                    params.put("systemPrompt", systemTpl);
                    params.put("userPrompt", userTpl);
                    break;
                case HTTP:
                    String method = str(cfg.getOrDefault("method", "GET")).toUpperCase(Locale.ROOT);
                    String url = WorkflowVarSupport.applyVars(str(cfg.get("url")), vars);
                    boolean bodyMethod = "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method);
                    if (!bodyMethod) {
                        url = WorkflowHttpActivity.appendQueryParams(
                                url, WorkflowHttpActivity.resolveFormFields(cfg, vars));
                    }
                    params.put("method", method.isBlank() ? "GET" : method);
                    params.put("url", url);
                    params.put("bodyType", cfg.getOrDefault("bodyType", bodyMethod ? "json" : "form"));
                    params.put("timeoutSec", cfg.get("timeoutSec"));
                    params.put("headers", WorkflowHttpActivity.resolveHeaders(cfg, vars));
                    if (cfg.get("formFields") != null) {
                        params.put("formFields", cfg.get("formFields"));
                    } else if (bodyMethod) {
                        params.put("body", str(cfg.get("body")));
                    }
                    break;
                case KNOWLEDGE:
                    params.put("knowledgeId", cfg.get("knowledgeId"));
                    params.put("query", str(cfg.get("query")));
                    params.put("topK", cfg.get("topK"));
                    params.put("scoreThreshold", cfg.get("scoreThreshold"));
                    break;
                case OUTPUT:
                    String tpl = str(cfg.get("template"));
                    if (tpl.isBlank()) {
                        tpl = "{output}";
                    }
                    String outType = str(cfg.get("type"));
                    if (outType.isBlank()) {
                        outType = "text";
                    }
                    params.put("type", outType.toLowerCase(Locale.ROOT));
                    params.put("template", tpl);
                    break;
                case CODE:
                    params.put("language", cfg.getOrDefault("language", "js"));
                    params.put("script", str(cfg.get("script")));
                    break;
                case SQL:
                    params.put("sql", str(cfg.get("sql")));
                    break;
                default:
                    // START/END/CONDITION 已在上方处理
                    break;
            }
        }
        return resolveParamsVars(params, vars);
    }

    /** 过程参数中所有字符串的 {var} 替换为实际值（含嵌套 Map/List） */
    private static Map<String, Object> resolveParamsVars(Map<String, Object> params, Map<String, String> vars) {
        if (params == null || params.isEmpty()) {
            return params == null ? new LinkedHashMap<>() : params;
        }
        Object resolved = applyVarsDeep(params, vars);
        if (resolved instanceof Map<?, ?> map) {
            Map<String, Object> out = new LinkedHashMap<>();
            map.forEach((k, v) -> {
                if (k != null) {
                    out.put(String.valueOf(k), v);
                }
            });
            return out;
        }
        return params;
    }

    private static Object applyVarsDeep(Object value, Map<String, String> vars) {
        if (value == null) {
            return null;
        }
        if (value instanceof String s) {
            return truncateText(WorkflowVarSupport.applyVars(s, vars));
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (e.getKey() == null) {
                    continue;
                }
                out.put(String.valueOf(e.getKey()), applyVarsDeep(e.getValue(), vars));
            }
            return out;
        }
        if (value instanceof List<?> list) {
            List<Object> out = new ArrayList<>(list.size());
            for (Object o : list) {
                out.add(applyVarsDeep(o, vars));
            }
            return out;
        }
        return value;
    }

    private static Map<String, String> truncateMap(Map<String, String> src) {
        Map<String, String> out = new LinkedHashMap<>();
        if (src == null) {
            return out;
        }
        for (Map.Entry<String, String> e : src.entrySet()) {
            if (e.getKey() == null) {
                continue;
            }
            out.put(e.getKey(), truncateText(e.getValue()));
        }
        return out;
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

    @SuppressWarnings("unchecked")
    private void putVar(FlowContext ctx, String key, String val) {
        ctx.put(key, val);
        Map<String, String> vars = new LinkedHashMap<>();
        Object raw = ctx.get(CTX_VARS);
        if (raw instanceof Map<?, ?> map) {
            map.forEach((k, v) -> {
                if (k != null) {
                    vars.put(String.valueOf(k), v == null ? "" : String.valueOf(v));
                }
            });
        }
        vars.put(key, val == null ? "" : val);
        ctx.put(CTX_VARS, vars);
    }

    private void handleCondition(FlowContext ctx, String key, String name, String config,
                                 List<WorkflowRunResult.Step> steps, WorkflowRunListener bus) {
        long t0 = System.currentTimeMillis();
        Map<String, String> inputVars = varsOf(ctx);
        AgentWorkflowCondition.Spec spec = AgentWorkflowCondition.parse(config);
        String branch = AgentWorkflowCondition.pickBranch(spec,
                expr -> codeEngineSupport.evalJsCondition(expr, inputVars));
        ctx.put(CTX_BRANCH, branch);
        WorkflowRunResult.Step step = new WorkflowRunResult.Step();
        step.setNodeKey(key);
        step.setNodeType(WorkflowNodeType.CONDITION.getCode());
        step.setNodeName(name);
        step.setInput(truncateMap(inputVars));
        Map<String, Object> params = new LinkedHashMap<>();
        List<Map<String, Object>> cases = new ArrayList<>();
        for (AgentWorkflowCondition.Case c : spec.cases()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", c.id());
            row.put("match", c.match());
            cases.add(row);
        }
        params.put("cases", cases);
        params.put("elseId", spec.elseId());
        params.put("branch", branch);
        step.setParams(params);
        step.setOutput(branch);
        String hit = spec.cases().stream().filter(c -> c.id().equals(branch)).findFirst()
                .map(c -> "如果「" + c.match() + "」")
                .orElse("否则");
        step.setDetail("走「" + hit + "」分支 (" + branch + ")");
        step.setDurationMs(Math.max(0, System.currentTimeMillis() - t0));
        steps.add(step);
        bus.onStep(step);
    }

    private void emitAnswerChunked(FlowContext ctx, String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        // 非模型节点：按块推送，模拟流式回答
        int size = 48;
        for (int i = 0; i < text.length(); i += size) {
            int end = Math.min(text.length(), i + size);
            emitAnswerDelta(ctx, text.substring(i, end));
        }
    }

    private void emitAnswerDelta(FlowContext ctx, String delta) {
        if (delta == null || delta.isEmpty()) {
            return;
        }
        Object raw = ctx.get(CTX_ANSWER);
        if (raw instanceof StringBuilder sb) {
            sb.append(delta);
        }
        listenerOf(ctx).onAnswerDelta(delta);
    }

    private static String answerOf(FlowContext ctx) {
        Object raw = ctx.get(CTX_ANSWER);
        if (raw instanceof StringBuilder sb) {
            return sb.toString();
        }
        return raw == null ? "" : String.valueOf(raw);
    }

    private static WorkflowRunListener listenerOf(FlowContext ctx) {
        Object raw = ctx.get(CTX_LISTENER);
        if (raw instanceof WorkflowRunListener l) {
            return l;
        }
        return new WorkflowRunListener() {
        };
    }

    private Map<String, Object> parseLlmConfig(String config) {
        if (config == null || config.isBlank()) {
            return Map.of();
        }
        return AgentJson.parseMap(config.trim());
    }

    private ChatModel resolveChatModel(Long agentId) {
        return chatModelSupport.resolveChatModel(agentId);
    }

    private Map<String, String> varsOf(FlowContext ctx) {
        return WorkflowVarSupport.varsOf(ctx, CTX_OUTPUT, CTX_VARS);
    }

    /** 开始节点入参快照，不含尚未产生的 output */
    private static Map<String, String> startVarsOf(FlowContext ctx) {
        Map<String, String> vars = new LinkedHashMap<>();
        Object raw = ctx.get(CTX_VARS);
        if (raw instanceof Map<?, ?> map) {
            map.forEach((k, v) -> {
                if (k != null) {
                    vars.put(String.valueOf(k), v == null ? "" : String.valueOf(v));
                }
            });
        }
        return vars;
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

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private static String rootMessage(Throwable e) {
        Throwable cur = e;
        String msg = e.getMessage();
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
            if (cur.getMessage() != null && !cur.getMessage().isBlank()) {
                msg = cur.getMessage();
            }
        }
        return msg == null ? e.getClass().getSimpleName() : msg;
    }
}
