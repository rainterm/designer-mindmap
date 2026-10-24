package com.designer.admin.module.agent.workflow;

import org.noear.solon.flow.TaskComponent;

/**
 * 按节点类型提供 Solon Flow 任务实现。
 *
 * @author 雨夜终端
 */
public interface AgentWorkflowNodeTasks {

    /**
     * 构建活动节点任务（LLM / HTTP / SQL / 代码 / 知识库 / 输出等）。
     *
     * @param key       节点 key
     * @param type      节点类型 code
     * @param name      节点显示名
     * @param config    节点 JSON 配置
     * @param outputVar 输出变量名；输出节点可为空
     * @return Solon Flow 任务组件
     */
    TaskComponent activityTask(String key, String type, String name, String config, String outputVar);

    /**
     * 构建条件（排他）节点任务。
     *
     * @param key    节点 key
     * @param name   节点显示名
     * @param config 条件配置 JSON 或旧版单表达式
     * @return Solon Flow 任务组件
     */
    TaskComponent conditionTask(String key, String name, String config);
}
