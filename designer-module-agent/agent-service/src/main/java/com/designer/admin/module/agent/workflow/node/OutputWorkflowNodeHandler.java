package com.designer.admin.module.agent.workflow.node;

import com.designer.admin.module.agent.constant.workflow.WorkflowNodeType;
import com.designer.admin.module.agent.workflow.activity.WorkflowOutputActivity;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

/**
 * 工作流输出节点处理器
 *
 * @author 雨夜终端
 */
@Component
public class OutputWorkflowNodeHandler implements WorkflowNodeHandler {

    @Inject
    private WorkflowOutputActivity outputActivity;

    @Override
    public WorkflowNodeType type() {
        return WorkflowNodeType.OUTPUT;
    }

    @Override
    public String execute(WorkflowNodeContext ctx) {
        String out = outputActivity.runOutput(ctx.config(), ctx.vars());
        ctx.emitAnswerChunked(out);
        return out;
    }
}
