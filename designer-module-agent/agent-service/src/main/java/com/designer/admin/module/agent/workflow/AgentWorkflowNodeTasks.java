package com.designer.admin.module.agent.workflow;

import org.noear.solon.flow.TaskComponent;

/**
 * 按节点类型提供 Solon Flow 任务实现。
 *
 * @author 雨夜终端
 */
public interface AgentWorkflowNodeTasks {

    TaskComponent activityTask(String key, String type, String name, String config, String outputVar);

    TaskComponent conditionTask(String key, String name, String config);
}
