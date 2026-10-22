package com.designer.admin.module.agent.workflow.node;

import com.designer.admin.module.agent.constant.workflow.WorkflowNodeType;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 按 {@link WorkflowNodeType} 解析节点处理器；新增 Handler 只需声明为 Bean。
 *
 * @author 雨夜终端
 */
@Component
public class WorkflowNodeHandlerRegistry {

    private final Map<WorkflowNodeType, WorkflowNodeHandler> byType = new EnumMap<>(WorkflowNodeType.class);

    @Inject
    public void setHandlers(List<WorkflowNodeHandler> handlers) {
        byType.clear();
        if (handlers == null) {
            return;
        }
        for (WorkflowNodeHandler h : handlers) {
            if (h == null || h.type() == null) {
                continue;
            }
            byType.put(h.type(), h);
        }
    }

    public WorkflowNodeHandler get(WorkflowNodeType type) {
        return type == null ? null : byType.get(type);
    }
}
