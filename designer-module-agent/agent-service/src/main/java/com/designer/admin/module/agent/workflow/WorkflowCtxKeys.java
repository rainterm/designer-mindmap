package com.designer.admin.module.agent.workflow;

import com.designer.admin.module.agent.constant.AgentConsts;

/**
 * 工作流 FlowContext 键名，供 Executor / Handler 共用，避免 Handler 反向依赖 Executor。
 *
 * @author 雨夜终端
 */
public final class WorkflowCtxKeys {

    /** 流水线当前输入（通常为上一节点输出或开始节点种子） */
    public static final String INPUT = AgentConsts.CTX_INPUT;
    /** 流水线当前输出 */
    public static final String OUTPUT = AgentConsts.CTX_OUTPUT;
    /** 条件节点选出的分支 id */
    public static final String BRANCH = "branch";
    /** 试跑步骤列表 */
    public static final String STEPS = "_steps";
    /** 绑定智能体解析出的 ChatModel */
    public static final String CHAT_MODEL = "_chatModel";
    /** 开始节点解析后的全部入参 */
    public static final String VARS = "_vars";
    /** 对外回答内容累积 */
    public static final String ANSWER = "_answer";
    /** 执行过程回调监听器 */
    public static final String LISTENER = "_listener";

    /** 工具类，禁止实例化。 */
    private WorkflowCtxKeys() {
    }
}
