package com.designer.admin.module.agent.workflow.node;

import com.designer.admin.module.agent.constant.workflow.WorkflowNodeType;

import java.util.Map;

/**
 * 工作流活动节点处理器。新增节点类型时实现本接口并注册为 {@code @Component}。
 *
 * @author 雨夜终端
 */
public interface WorkflowNodeHandler {

    /**
     * 返回本处理器对应的节点类型。
     *
     * @return 节点类型枚举，不可为 null
     */
    WorkflowNodeType type();

    /**
     * 执行节点业务逻辑。
     *
     * @param ctx 节点执行上下文（配置、变量、步骤元数据与推送回调）
     * @return 节点输出文本；无输出时可为 null
     * @throws Exception 节点执行失败时抛出
     */
    String execute(WorkflowNodeContext ctx) throws Exception;

    /**
     * 试跑 SSE step.params 的类型相关字段（不含 outputVar；变量替换由 Executor 统一做）。
     *
     * @param configJson 节点配置 JSON
     * @param vars       当前工作流变量表
     * @return 用于步骤面板展示的参数 Map；默认空 Map
     */
    default Map<String, Object> describeParams(String configJson, Map<String, String> vars) {
        return Map.of();
    }
}
