package com.designer.admin.module.agent.workflow.node;

import com.designer.admin.module.agent.constant.workflow.WorkflowNodeType;
import com.designer.admin.module.agent.workflow.activity.WorkflowSqlActivity;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

/**
 * 工作流 SQL 节点处理器
 *
 * @author 雨夜终端
 */
@Component
public class SqlWorkflowNodeHandler implements WorkflowNodeHandler {

    @Inject
    private WorkflowSqlActivity sqlActivity;

    @Override
    public WorkflowNodeType type() {
        return WorkflowNodeType.SQL;
    }

    @Override
    public String execute(WorkflowNodeContext ctx) throws Exception {
        return sqlActivity.runSql(ctx.config(), ctx.vars());
    }
}
