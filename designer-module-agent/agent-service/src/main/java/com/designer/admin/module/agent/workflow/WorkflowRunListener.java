package com.designer.admin.module.agent.workflow;

import com.designer.admin.module.agent.model.vo.workflow.WorkflowRunResult;

/**
 * 工作流执行过程回调（试跑 SSE / 智能体对话流式）。
 *
 * @author 雨夜终端
 */
public interface WorkflowRunListener {

    /**
     * 状态文案变更（如「正在执行工作流…」「执行节点：xxx」）。
     *
     * @param status 状态文案
     */
    default void onStatus(String status) {
    }

    /**
     * 单个节点步骤完成（含开始 / 结束 / 活动 / 条件）。
     *
     * @param step 步骤快照
     */
    default void onStep(WorkflowRunResult.Step step) {
    }

    /**
     * 回答内容增量（仅 emitAnswer / 输出节点）。
     *
     * @param delta 增量文本
     */
    default void onAnswerDelta(String delta) {
    }

    /**
     * 思考过程增量（LLM 开启 emitThinking）。
     *
     * @param delta 思考增量文本
     */
    default void onThinkingDelta(String delta) {
    }

    /**
     * 执行失败时的错误消息。
     *
     * @param message 错误文案
     */
    default void onError(String message) {
    }
}
