package com.designer.admin.system.controller.admin.message;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.designer.admin.common.core.PageQuery;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.Result;
import com.designer.admin.framework.message.NoticeSseHub;
import com.designer.admin.framework.satoken.LoginHelper;
import com.designer.admin.system.model.entity.message.SysNotice;
import com.designer.admin.system.model.vo.message.NoticeReadCountVO;
import com.designer.admin.system.service.message.SysNoticeService;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Post;
import org.noear.solon.validation.annotation.Valid;
import org.noear.solon.web.sse.SseEmitter;


/**
 * 站内消息接收（顶部消息中心 + SSE）
 *
 * @author 雨夜终端
 */
@Valid
@Controller
@Mapping("/admin-api/system/notice")
@Api(tags = "站内通知")
public class SysNoticeController {

    @Inject
    private SysNoticeService noticeService;

    @Inject
    private NoticeSseHub noticeSseHub;

    @Get
    @Mapping("/inbox")
    @ApiOperation(value = "分页查询站内通知")
    public Result<PageResult<SysNotice>> listInbox(PageQuery query) {
        return Result.ok(PageResult.of(noticeService.listInboxPage(query)));
    }

    @Get
    @Mapping("/unread-count")
    @ApiOperation(value = "统计未读通知数量")
    public Result<Long> countUnread() {
        return Result.ok(noticeService.countUnread());
    }

    @Post
    @Mapping("/read-all")
    @ApiOperation(value = "全部标记为已读")
    public Result<NoticeReadCountVO> markAllRead() {
        return Result.ok(new NoticeReadCountVO(noticeService.markAllRead()));
    }

    @Post
    @Mapping("/{id}/read")
    @ApiOperation(value = "标记通知为已读")
    public Result<Void> markRead(Long id) {
        noticeService.markRead(id);
        return Result.ok();
    }

    @Get
    @Mapping("/sse")
    @SaCheckLogin
    @ApiOperation(value = "订阅通知 SSE 事件流")
    public SseEmitter sse() {
        // token 由 SseQueryTokenFilter 从 ?Authorization= 提升到请求头；心跳时用它复核登录态
        return noticeSseHub.open(LoginHelper.getUserId(), StpUtil.getTokenValue());
    }
}
