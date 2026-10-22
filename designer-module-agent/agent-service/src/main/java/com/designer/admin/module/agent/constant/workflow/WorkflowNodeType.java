package com.designer.admin.module.agent.constant.workflow;

/**
 * 工作流节点类型。
 *
 * @author 雨夜终端
 */
public enum WorkflowNodeType {

    /** 开始 */
    START("start"),
    /** 结束 */
    END("end"),
    /** 大模型 */
    LLM("llm"),
    /** HTTP 请求 */
    HTTP("http"),
    /** 知识库检索 */
    KNOWLEDGE("knowledge"),
    /** 代码执行 */
    CODE("code"),
    /** SQL 查询 */
    SQL("sql"),
    /** 输出组装 */
    OUTPUT("output"),
    /** 条件分支 */
    CONDITION("condition");

    private final String code;

    WorkflowNodeType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean eq(String value) {
        return code.equals(value);
    }

    public boolean eqIgnoreCase(String value) {
        return value != null && code.equalsIgnoreCase(value);
    }

    public static WorkflowNodeType fromCode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (WorkflowNodeType t : values()) {
            if (t.code.equalsIgnoreCase(value.trim())) {
                return t;
            }
        }
        return null;
    }
}
