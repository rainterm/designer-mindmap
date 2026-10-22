package com.designer.admin.module.agent.workflow.node;

import com.designer.admin.module.agent.constant.workflow.WorkflowNodeType;
import com.designer.admin.module.agent.workflow.activity.WorkflowHttpActivity;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

/**
 * 工作流 HTTP 节点处理器
 *
 * @author 雨夜终端
 */
@Component
public class HttpWorkflowNodeHandler implements WorkflowNodeHandler {

    @Inject
    private WorkflowHttpActivity httpActivity;

    @Override
    public WorkflowNodeType type() {
        return WorkflowNodeType.HTTP;
    }

    @Override
    public String execute(WorkflowNodeContext ctx) throws Exception {
        return httpActivity.runHttp(ctx.config(), ctx.vars());
    }
}
