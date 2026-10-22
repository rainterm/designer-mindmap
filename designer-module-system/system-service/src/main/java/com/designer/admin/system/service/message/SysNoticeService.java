package com.designer.admin.system.service.message;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.common.core.PageQuery;
import com.designer.admin.framework.message.InboxSink;
import com.designer.admin.system.model.entity.message.SysNotice;

/**
 * 站内消息：通道 inbox 投递落库 + 顶部消息中心 + SSE 推送；实现 framework {@link InboxSink}。
 *
 * @author 雨夜终端
 */
public interface SysNoticeService {

    /**
     * 发布站内通知：按接收人落库并推送。
     *
     * @param title     标题
     * @param content   内容
     * @param receivers 接收人：空=全体；否则逗号分隔登录账号或用户 ID
     * @return 通知 ID
     */
    Long publish(String title, String content, String receivers);

    /**
     * 发布站内通知，并指定点击后的跳转路由。
     *
     * @param title     标题
     * @param content   内容
     * @param receivers 接收人：空=全体；否则逗号分隔登录账号或用户 ID
     * @param link      前端路由 path
     * @return 通知 ID
     */
    Long publish(String title, String content, String receivers, String link);

    /**
     * 站内通道发送。
     *
     * @param title     标题
     * @param content   内容
     * @param receivers 接收人：空=全体；否则逗号分隔登录账号或用户 ID
     * @param link      前端路由 path，写入 remark
     * @return 通知 ID
     */
    Long publishInbox(String title, String content, String receivers, String link);

    /**
     * 站内通道发送（兼容旧调用，不指定跳转路由）。
     *
     * @param title     标题
     * @param content   内容
     * @param receivers 接收人：空=全体；否则逗号分隔登录账号或用户 ID
     * @return 通知 ID
     */
    Long publishInbox(String title, String content, String receivers);

    /**
     * 当前用户的消息中心分页。
     *
     * @param query 分页参数
     * @return 消息分页结果
     */
    IPage<SysNotice> listInboxPage(PageQuery query);

    /**
     * 当前用户的未读消息数。
     *
     * @return 未读条数
     */
    long countUnread();

    /**
     * 把单条消息标记为已读。
     *
     * @param noticeId 通知 ID
     */
    void markRead(Long noticeId);

    /**
     * 把当前用户的全部消息标记为已读。
     *
     * @return 影响条数
     */
    int markAllRead();
}
