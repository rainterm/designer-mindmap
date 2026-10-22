package com.designer.admin.module.agent.workflow.node;

import com.designer.admin.module.agent.constant.workflow.WorkflowNodeType;
/**
 * 工作流活动节点处理器。新增节点类型时实现本接口并注册为 {@code @Component}。
 *
 * @author 雨夜终端
 */
public interface WorkflowNodeHandler {

    WorkflowNodeType type();

    /**
     * 执行节点。
     *
     * @return 节点输出文本；无输出时可为 null
     */
    String execute(WorkflowNodeContext ctx) throws Exception;
}
