package com.designer.admin.module.agent.workflow;

import org.noear.solon.Utils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.module.agent.mapper.workflow.AgentWorkflowMapper;
import com.designer.admin.module.agent.model.entity.agent.AgentAgent;
import com.designer.admin.module.agent.model.entity.workflow.AgentWorkflow;
import com.designer.admin.module.agent.model.vo.workflow.WorkflowRunResult;
import com.designer.admin.module.agent.util.AgentJsonUtil;
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
import com.designer.admin.module.agent.constant.workflow.WorkflowNodeType;
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

    /** 日志 */
    private static final Logger log = LoggerFactory.getLogger(AgentWorkflowExecutor.class);

    /** 工作流 Mapper */
    @Db
    private AgentWorkflowMapper workflowMapper;
    /** 代码 / 条件表达式引擎 */
    @Inject
    private WorkflowCodeEngineSupport codeEngineSupport;
    /** 节点类型 → Handler 注册表 */
    @Inject
    private WorkflowNodeHandlerRegistry nodeHandlerRegistry;
    /** ChatModel 解析支持 */
    @Inject
    private AgentWorkflowChatModelSupport chatModelSupport;

    /**
     * 管理端试跑：优先草稿图，无监听器。
     *
     * @param workflowId 工作流 id
     * @param params     开始节点入参
     * @param agentId    绑定智能体 id（用于解析 ChatModel）
     * @return 执行结果
     */
    public WorkflowRunResult run(Long workflowId, Map<String, Object> params, Long agentId) {
        return run(workflowId, params, agentId, null, true);
    }

    /**
     * 管理端试跑：优先草稿图，可带过程回调。
     *
     * @param workflowId 工作流 id
     * @param params     开始节点入参
     * @param agentId    绑定智能体 id
     * @param listener   过程回调；可为 null
     * @return 执行结果
     */
    public WorkflowRunResult run(Long workflowId, Map<String, Object> params, Long agentId,
                                 WorkflowRunListener listener) {
        return run(workflowId, params, agentId, listener, true);
    }

    /**
     * 执行工作流。
     *
     * @param workflowId 工作流 id
     * @param params     开始节点入参
     * @param agentId    绑定智能体 id
     * @param listener   过程回调；可为 null
     * @param useDraft   true=管理端试跑（优先草稿图）；false=开放接口（仅已发布正式图）
     * @return 执行结果
     * @throws BizException 工作流不存在、停用或未发布时抛出
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
            wf.setNodesJson(Utils.isNotBlank(wf.getDraftNodesJson()) ? wf.getDraftNodesJson() : wf.getNodesJson());
            wf.setEdgesJson(Utils.isNotBlank(wf.getDraftEdgesJson()) ? wf.getDraftEdgesJson() : wf.getEdgesJson());
        } else if (Utils.isBlank(wf.getNodesJson())) {
            throw new BizException("工作流尚未发布");
        }
        ChatModel chatModel = resolveChatModel(agentId);
        return execute(wf, params, chatModel, listener);
    }

    /**
     * 供智能体对话：用户消息写入开始节点第一个参数，按绑定工作流依次执行。
     *
     * @param agent       智能体（含 workflowIdsJson）
     * @param userMessage 用户消息
     * @return 最后一个工作流输出；无绑定或均跳过时 null
     */
    public String runBound(AgentAgent agent, String userMessage) {
        return runBound(agent, userMessage, null);
    }

    /**
     * 供智能体对话：用户消息写入开始节点第一个参数，按绑定工作流依次执行。
     *
     * @param agent       智能体（含 workflowIdsJson）
     * @param userMessage 用户消息
     * @param listener    过程回调；可为 null
     * @return 最后一个工作流输出；无绑定或均跳过时 null
     */
    public String runBound(AgentAgent agent, String userMessage, WorkflowRunListener listener) {
        List<Long> ids = AgentJsonUtil.parseLongList(agent.getWorkflowIdsJson());
        if (ids.isEmpty()) {
            return null;
        }
        List<AgentWorkflow> list = workflowMapper.selectList(new LambdaQueryWrapper<AgentWorkflow>()
                        .in(AgentWorkflow::getId, ids)
                        .eq(AgentWorkflow::getStatus, SystemConsts.STATUS_ENABLE)
                        .orderByAsc(AgentWorkflow::getId))
                // 只执行已发布的工作流：正式图为空（从未发布）跳过
                .stream()
                .filter(wf -> Utils.isNotBlank(wf.getNodesJson()))
                .toList();
        if (list == null || list.isEmpty()) {
            return null;
        }
        ChatModel chatModel = resolveChatModel(agent.getId());
        String current = userMessage == null ? "" : userMessage;
        String lastOut = null;
        for (AgentWorkflow wf : list) {
            List<Map<String, Object>> nodes = AgentJsonUtil.parseMapList(wf.getNodesJson());
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

    /**
     * 查询指定 id 列表中启用且已发布的管理工作流（按 id 升序）。
     *
     * @param ids 工作流 id 列表
     * @return 启用且正式图非空的工作流；ids 为空时返回空列表
     */
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
                : list.stream().filter(wf -> Utils.isNotBlank(wf.getNodesJson())).toList();
    }

    /**
     * 供工作流工具回调：按 LLM 提供的参数执行工作流。
     *
     * @param wf        工作流实体（使用正式图）
     * @param params    开始节点入参
     * @param chatModel 会话模型；可为 null
     * @param listener  过程回调；可为 null
     * @return 执行结果
     */
    public WorkflowRunResult executeForTool(AgentWorkflow wf, Map<String, Object> params,
                                            ChatModel chatModel, WorkflowRunListener listener) {
        return execute(wf, params, chatModel, listener);
    }

    /**
     * 核心执行：构图、注入上下文、跑 FlowEngine、汇总回答与步骤。
     *
     * @param wf        工作流实体（nodesJson / edgesJson 已选定）
     * @param params    开始节点入参
     * @param chatModel 会话模型；可为 null
     * @param listener  过程回调；可为 null（内部用空实现）
     * @return 执行结果
     * @throws BizException 执行失败时包装抛出
     */
    private WorkflowRunResult execute(AgentWorkflow wf, Map<String, Object> params, ChatModel chatModel,
                                      WorkflowRunListener listener) {
        List<Map<String, Object>> nodes = AgentJsonUtil.parseMapList(wf.getNodesJson());
        List<Map<String, Object>> edges = AgentJsonUtil.parseMapList(wf.getEdgesJson());
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
        context.put(WorkflowCtxKeys.INPUT, seed);
        context.put(WorkflowCtxKeys.OUTPUT, "");
        context.put(WorkflowCtxKeys.BRANCH, "yes");
        context.put(WorkflowCtxKeys.STEPS, steps);
        context.put(WorkflowCtxKeys.VARS, new LinkedHashMap<>(vars));
        context.put(WorkflowCtxKeys.ANSWER, new StringBuilder());
        context.put(WorkflowCtxKeys.LISTENER, bus);
        for (Map.Entry<String, String> e : vars.entrySet()) {
            context.put(e.getKey(), e.getValue());
        }
        if (chatModel != null) {
            context.put(WorkflowCtxKeys.CHAT_MODEL, chatModel);
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
            Object out = context.get(WorkflowCtxKeys.OUTPUT);
            answer = out == null ? "" : String.valueOf(out);
        }
        result.setOutput(answer);
        result.setSteps(new ArrayList<>(steps));
        return result;
    }

    /** 步骤面板文本截断上限 */
    private static final int STEP_TEXT_LIMIT = 8000;

    /**
     * 处理开始节点：记录入参快照与步骤。
     *
     * @param ctx    Flow 上下文
     * @param key    节点 key
     * @param name   节点名
     * @param config 开始节点配置
     * @param steps  步骤列表（追加）
     */
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
        step.setOutput(truncateText(AgentJsonUtil.toJson(inputVars)));
        step.setDetail("已注入 " + inputVars.size() + " 个输入变量");
        step.setDurationMs(Math.max(0, System.currentTimeMillis() - t0));
        steps.add(step);
        bus.onStatus("执行节点：" + step.getNodeName());
        bus.onStep(step);
    }

    /**
     * 处理结束节点：汇总流水线输出与回答。
     *
     * @param ctx    Flow 上下文
     * @param key    节点 key
     * @param name   节点名
     * @param config 结束节点配置
     * @param steps  步骤列表（追加）
     */
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
        String pipelineOut = str(ctx.get(WorkflowCtxKeys.OUTPUT));
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

    /**
     * 处理活动节点：委托对应 Handler 执行并写回输出。
     *
     * @param ctx       Flow 上下文
     * @param key       节点 key
     * @param type      节点类型 code
     * @param name      节点名
     * @param config    节点配置
     * @param outputVar 输出变量名
     * @param steps     步骤列表（追加）
     * @throws Throwable Handler 或业务异常向上抛出
     */
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

    /**
     * 将节点输出写入流水线 OUTPUT 与可选变量，并截断写入步骤。
     *
     * @param ctx       Flow 上下文
     * @param outputVar 输出变量名；空白则只写 OUTPUT
     * @param text      节点输出文本
     * @param step      当前步骤（写 output 字段）
     */
    private void writeNodeOutput(FlowContext ctx, String outputVar, String text, WorkflowRunResult.Step step) {
        String t = text == null ? "" : text;
        ctx.put(WorkflowCtxKeys.OUTPUT, t);
        String var = outputVar == null ? "" : outputVar.trim();
        if (!var.isBlank() && var.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            putVar(ctx, var, t);
        }
        step.setOutput(truncateText(t));
    }

    /**
     * 组装步骤面板 params（outputVar + Handler.describeParams，并做变量替换）。
     *
     * @param type      节点类型 code
     * @param config    节点配置
     * @param outputVar 输出变量名
     * @param vars      当前变量表
     * @return 步骤参数 Map
     */
    private Map<String, Object> buildStepParams(String type, String config, String outputVar,
                                                Map<String, String> vars) {
        Map<String, Object> params = new LinkedHashMap<>();
        if (outputVar != null && !outputVar.isBlank()) {
            params.put("outputVar", outputVar.trim());
        }
        WorkflowNodeType nodeType = WorkflowNodeType.fromCode(type);
        WorkflowNodeHandler handler = nodeType == null ? null : nodeHandlerRegistry.get(nodeType);
        if (handler != null) {
            Map<String, Object> described = handler.describeParams(config, vars);
            if (described != null && !described.isEmpty()) {
                params.putAll(described);
            }
        } else if (config != null && !config.isBlank()) {
            Map<String, Object> cfg = AgentJsonUtil.parseMap(config.trim());
            if (!cfg.isEmpty()) {
                params.put("config", cfg);
            } else {
                params.put("config", config.trim());
            }
        }
        return resolveParamsVars(params, vars);
    }

    /**
     * 过程参数中所有字符串的 {var} 替换为实际值（含嵌套 Map/List）。
     *
     * @param params 原始参数
     * @param vars   变量表
     * @return 替换后的参数 Map
     */
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

    /**
     * 递归对 String / Map / List 做变量替换与截断。
     *
     * @param value 任意值
     * @param vars  变量表
     * @return 处理后的值
     */
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

    /**
     * 截断 Map 中各 value 文本长度。
     *
     * @param src 源 Map
     * @return 新 Map（value 已截断）
     */
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

    /**
     * 截断过长文本，超出部分附带总字数提示。
     *
     * @param s 原文
     * @return 截断后文本
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

    /**
     * 写入上下文键，并同步更新 {@link WorkflowCtxKeys#VARS}。
     *
     * @param ctx Flow 上下文
     * @param key 变量名
     * @param val 变量值
     */
    @SuppressWarnings("unchecked")
    private void putVar(FlowContext ctx, String key, String val) {
        ctx.put(key, val);
        Map<String, String> vars = new LinkedHashMap<>();
        Object raw = ctx.get(WorkflowCtxKeys.VARS);
        if (raw instanceof Map<?, ?> map) {
            map.forEach((k, v) -> {
                if (k != null) {
                    vars.put(String.valueOf(k), v == null ? "" : String.valueOf(v));
                }
            });
        }
        vars.put(key, val == null ? "" : val);
        ctx.put(WorkflowCtxKeys.VARS, vars);
    }

    /**
     * 处理条件节点：求值表达式、写入 BRANCH、记录步骤。
     *
     * @param ctx    Flow 上下文
     * @param key    节点 key
     * @param name   节点名
     * @param config 条件配置
     * @param steps  步骤列表（追加）
     * @param bus    过程回调
     */
    private void handleCondition(FlowContext ctx, String key, String name, String config,
                                 List<WorkflowRunResult.Step> steps, WorkflowRunListener bus) {
        long t0 = System.currentTimeMillis();
        Map<String, String> inputVars = varsOf(ctx);
        AgentWorkflowCondition.Spec spec = AgentWorkflowCondition.parse(config);
        String branch = AgentWorkflowCondition.pickBranch(spec,
                expr -> codeEngineSupport.evalJsCondition(expr, inputVars));
        ctx.put(WorkflowCtxKeys.BRANCH, branch);
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

    /**
     * 非模型节点：按块推送回答，模拟流式效果。
     *
     * @param ctx  Flow 上下文
     * @param text 完整回答文本
     */
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

    /**
     * 累积回答增量并回调监听器。
     *
     * @param ctx   Flow 上下文
     * @param delta 增量文本
     */
    private void emitAnswerDelta(FlowContext ctx, String delta) {
        if (delta == null || delta.isEmpty()) {
            return;
        }
        Object raw = ctx.get(WorkflowCtxKeys.ANSWER);
        if (raw instanceof StringBuilder sb) {
            sb.append(delta);
        }
        listenerOf(ctx).onAnswerDelta(delta);
    }

    /**
     * 读取累积回答文本。
     *
     * @param ctx Flow 上下文
     * @return 回答全文
     */
    private static String answerOf(FlowContext ctx) {
        Object raw = ctx.get(WorkflowCtxKeys.ANSWER);
        if (raw instanceof StringBuilder sb) {
            return sb.toString();
        }
        return raw == null ? "" : String.valueOf(raw);
    }

    /**
     * 从上下文取监听器；缺失时返回空实现。
     *
     * @param ctx Flow 上下文
     * @return 非 null 监听器
     */
    private static WorkflowRunListener listenerOf(FlowContext ctx) {
        Object raw = ctx.get(WorkflowCtxKeys.LISTENER);
        if (raw instanceof WorkflowRunListener l) {
            return l;
        }
        return new WorkflowRunListener() {
        };
    }

    /**
     * 按智能体 id 解析 ChatModel。
     *
     * @param agentId 智能体 id
     * @return ChatModel；可为 null
     */
    private ChatModel resolveChatModel(Long agentId) {
        return chatModelSupport.resolveChatModel(agentId);
    }

    /**
     * 组装当前变量表（入参 + output）。
     *
     * @param ctx Flow 上下文
     * @return 变量表
     */
    private Map<String, String> varsOf(FlowContext ctx) {
        return WorkflowVarSupport.varsOf(ctx, WorkflowCtxKeys.OUTPUT, WorkflowCtxKeys.VARS);
    }

    /**
     * 开始节点入参快照，不含尚未产生的 output。
     *
     * @param ctx Flow 上下文
     * @return 入参 Map 副本
     */
    private static Map<String, String> startVarsOf(FlowContext ctx) {
        Map<String, String> vars = new LinkedHashMap<>();
        Object raw = ctx.get(WorkflowCtxKeys.VARS);
        if (raw instanceof Map<?, ?> map) {
            map.forEach((k, v) -> {
                if (k != null) {
                    vars.put(String.valueOf(k), v == null ? "" : String.valueOf(v));
                }
            });
        }
        return vars;
    }

    /**
     * 将对象转为字符串。
     *
     * @param o 任意对象
     * @return 非 null 字符串
     */
    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    /**
     * 沿 cause 链取最深层非空消息。
     *
     * @param e 异常
     * @return 根因消息或简单类名
     */
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
