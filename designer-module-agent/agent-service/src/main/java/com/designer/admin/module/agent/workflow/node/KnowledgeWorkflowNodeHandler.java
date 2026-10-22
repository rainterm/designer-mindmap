package com.designer.admin.module.agent.workflow.node;

import com.designer.admin.module.agent.constant.workflow.WorkflowNodeType;
import com.designer.admin.module.agent.workflow.activity.WorkflowKnowledgeActivity;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

/**
 * 工作流知识库节点处理器
 *
 * @author 雨夜终端
 */
@Component
public class KnowledgeWorkflowNodeHandler implements WorkflowNodeHandler {

    @Inject
    private WorkflowKnowledgeActivity knowledgeActivity;

    @Override
    public WorkflowNodeType type() {
        return WorkflowNodeType.KNOWLEDGE;
    }

    @Override
    public String execute(WorkflowNodeContext ctx) {
        return knowledgeActivity.runKnowledge(ctx.config(), ctx.vars());
    }
}
