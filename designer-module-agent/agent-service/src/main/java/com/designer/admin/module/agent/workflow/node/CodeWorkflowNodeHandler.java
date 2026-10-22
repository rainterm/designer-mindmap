package com.designer.admin.module.agent.workflow.node;

import com.designer.admin.module.agent.constant.workflow.WorkflowNodeType;
import com.designer.admin.module.agent.workflow.support.WorkflowCodeEngineSupport;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

/**
 * 工作流代码节点处理器
 *
 * @author 雨夜终端
 */
@Component
public class CodeWorkflowNodeHandler implements WorkflowNodeHandler {

    @Inject
    private WorkflowCodeEngineSupport codeEngineSupport;

    @Override
    public WorkflowNodeType type() {
        return WorkflowNodeType.CODE;
    }

    @Override
    public String execute(WorkflowNodeContext ctx) throws Exception {
        return codeEngineSupport.runCode(ctx.config(), ctx.vars());
    }
}
