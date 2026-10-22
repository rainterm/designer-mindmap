package com.designer.admin.framework.message;

/**
 * 站内信投递 SPI：由 system 模块实现，framework 的 inbox Provider 仅依赖此接口。
 *
 * @author 雨夜终端
 */
public interface InboxSink {

    /**
     * 发布站内通知。
     *
     * @param title     标题
     * @param content   内容
     * @param receivers 接收人：空串或 {@code *} 表示全体；否则逗号分隔的用户 ID 或登录账号
     * @return 通知 ID
     */
    Long publish(String title, String content, String receivers);

    /**
     * 发布站内通知（带前端跳转路径）。
     *
     * @param title     标题
     * @param content   内容
     * @param receivers 接收人：空串或 {@code *} 表示全体；否则逗号分隔的用户 ID 或登录账号
     * @param link      前端路由 path，如 /system/user；空则不跳转
     * @return 通知 ID
     */
    default Long publish(String title, String content, String receivers, String link) {
        return publish(title, content, receivers);
    }
}
