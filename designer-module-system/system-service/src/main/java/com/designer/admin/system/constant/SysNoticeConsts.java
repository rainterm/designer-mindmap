package com.designer.admin.system.constant;

/**
 * 消息中心 / 站内通知的固定值。
 *
 * <p>「全部接收人」与「通知跳转前缀」这类值同时被写入侧与展示侧使用：
 * 前者决定一条通知广播给谁，后者决定点击后跳到哪。散落在两个 impl 里，
 * 改一处就会出现「能收到、点不开」的通知。</p>
 *
 * @author 雨夜终端
 */
public final class SysNoticeConsts {

    /** 通知内跳转链接的前缀（前端据此识别站内路由） */
    public static final String LINK_PREFIX = "link:";

    /** 接收人为「全部用户」的标记 */
    public static final String RECEIVERS_ALL = "*";

    private SysNoticeConsts() {
    }
}
