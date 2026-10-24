package com.designer.admin.module.agent.harness.tool;

import com.designer.admin.module.agent.model.entity.workflow.AgentWorkflow;
import com.designer.admin.module.agent.util.AgentJsonUtil;
import com.designer.admin.module.agent.workflow.AgentWorkflowExecutor;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.ai.chat.ChatConfig;
import org.noear.solon.ai.chat.ChatModel;
import org.noear.solon.ai.chat.tool.FunctionTool;

import java.util.ArrayList;
import java.util.List;

/**
 * 将智能体绑定的管理工作流贡献为 FunctionTool。
 *
 * @author 雨夜终端
 */
@Component
public class WorkflowFunctionToolProvider implements FunctionToolProvider {

    /** 工作流执行器 */
    @Inject
    private AgentWorkflowExecutor workflowExecutor;

    /**
     * 智能体是否绑定了工作流。
     *
     * @param ctx 构建上下文
     * @return true 表示有可贡献的工作流
     */
    @Override
    public boolean supports(FunctionToolBuildContext ctx) {
        if (ctx == null || ctx.agent() == null) {
            return false;
        }
        List<Long> ids = AgentJsonUtil.parseLongList(ctx.agent().getWorkflowIdsJson());
        return ids != null && !ids.isEmpty();
    }

    /**
     * 按绑定的工作流 ID 列表构建 {@link WorkflowFunctionTool}。
     *
     * @param ctx 构建上下文
     * @return 工作流工具列表
     */
    @Override
    public List<FunctionTool> build(FunctionToolBuildContext ctx) {
        List<Long> ids = AgentJsonUtil.parseLongList(ctx.agent().getWorkflowIdsJson());
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<AgentWorkflow> list = workflowExecutor.listBoundWorkflows(ids);
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        ChatConfig chatConfig = ctx.chatConfig();
        ChatModel chatModel = chatConfig == null ? null : chatConfig.toChatModel();
        List<FunctionTool> tools = new ArrayList<>();
        for (AgentWorkflow wf : list) {
            tools.add(new WorkflowFunctionTool(wf, workflowExecutor, chatModel, ctx.onStatus()));
        }
        return tools;
    }
}
