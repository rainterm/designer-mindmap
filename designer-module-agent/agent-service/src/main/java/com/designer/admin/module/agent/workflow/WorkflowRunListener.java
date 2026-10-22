package com.designer.admin.module.agent.workflow;

import com.designer.admin.module.agent.model.vo.workflow.WorkflowRunResult;
/**
 * 工作流执行过程回调（试跑 SSE / 智能体对话流式）。
 *
 * @author 雨夜终端
 */
public interface WorkflowRunListener {

    default void onStatus(String status) {
    }

    default void onStep(WorkflowRunResult.Step step) {
    }

    /** 回答内容增量（仅 emitAnswer / 输出节点） */
    default void onAnswerDelta(String delta) {
    }

    /** 思考过程增量（LLM 开启 emitThinking） */
    default void onThinkingDelta(String delta) {
    }

    default void onError(String message) {
    }
}
