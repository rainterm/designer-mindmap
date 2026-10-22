package com.designer.admin.module.agent.constant.session;

/**
 * 智能体模块 SSE 事件名（对话 / 工作流试跑 / 文档索引）。
 *
 * @author 雨夜终端
 */
public enum AgentSseEvent {

    /** 用户消息事件 */
    USER("user"),
    /** 流式增量 */
    DELTA("delta"),
    /** 工具调用 */
    TOOL("tool"),
    /** 状态提示 */
    STATUS("status"),
    /** 思考过程 */
    THINKING("thinking"),
    /** 完成 */
    DONE("done"),
    /** 错误 */
    ERROR("error"),
    /** 工作流步骤 */
    STEP("step"),
    /** 索引进度 */
    PROGRESS("progress");

    private final String code;

    AgentSseEvent(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
