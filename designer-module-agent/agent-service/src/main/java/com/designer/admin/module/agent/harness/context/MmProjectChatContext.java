package com.designer.admin.module.agent.harness.context;

/**
 * 导图编辑页侧边对话的绑定上下文（不进入用户可见消息）。
 *
 * @author 雨夜终端
 */
public final class MmProjectChatContext {

    /** 当前线程绑定的导图项目 ID */
    private static final ThreadLocal<Long> PROJECT_ID = new ThreadLocal<>();

    /** 工具类，禁止实例化。 */
    private MmProjectChatContext() {
    }

    /**
     * 绑定当前线程的导图项目 ID；非法值则清除。
     *
     * @param projectId 导图项目 ID；null 或 ≤0 时清除绑定
     */
    public static void setProjectId(Long projectId) {
        if (projectId == null || projectId <= 0) {
            PROJECT_ID.remove();
        } else {
            PROJECT_ID.set(projectId);
        }
    }

    /**
     * 读取当前线程绑定的导图项目 ID。
     *
     * @return 项目 ID；未绑定则为 null
     */
    public static Long getProjectId() {
        return PROJECT_ID.get();
    }

    /** 清除当前线程的导图项目绑定。 */
    public static void clear() {
        PROJECT_ID.remove();
    }
}
