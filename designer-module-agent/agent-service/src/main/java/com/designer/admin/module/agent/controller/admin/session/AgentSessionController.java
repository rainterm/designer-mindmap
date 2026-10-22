package com.designer.admin.module.agent.controller.admin.session;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.Result;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.module.agent.model.dto.session.SendMessageDTO;
import com.designer.admin.module.agent.model.dto.session.SessionQueryDTO;
import com.designer.admin.module.agent.model.dto.session.SessionSaveDTO;
import com.designer.admin.module.agent.model.vo.session.ChatRecordVO;
import com.designer.admin.module.agent.model.vo.session.MessageVO;
import com.designer.admin.module.agent.model.vo.session.SendMessageResult;
import com.designer.admin.module.agent.model.vo.session.SessionVO;
import com.designer.admin.module.agent.service.session.AgentSessionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.noear.solon.annotation.Body;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Delete;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Post;
import org.noear.solon.validation.annotation.Valid;
import org.noear.solon.validation.annotation.Validated;
import org.noear.solon.web.sse.SseEmitter;

import java.util.List;
import org.noear.solon.validation.annotation.NotNull;
import org.noear.solon.validation.annotation.NotEmpty;

/**
 * 智能体会话。
 *
 * @author 雨夜终端
 */
@Valid
@Controller
@Mapping("/admin-api/agent/session")
@Api(tags = "智能体会话")
public class AgentSessionController {

    @Inject
    private AgentSessionService sessionService;

    @Get
    @Mapping("/page")
    @SaCheckPermission("agent:session:list")
    @ApiOperation(value = "分页查询会话记录")
    public Result<PageResult<ChatRecordVO>> page(SessionQueryDTO query) {
        return Result.ok(PageResult.of(sessionService.page(query)));
    }

    @Get
    @Mapping("/list")
    @SaCheckPermission("agent:session:chat")
    @ApiOperation(value = "查询当前用户在指定智能体下的会话列表")
    public Result<List<SessionVO>> list(@NotNull(message = "智能体ID不能为空") Long agentId) {
        return Result.ok(sessionService.listMineByAgent(agentId));
    }

    @Get
    @Mapping("/open")
    @SaCheckPermission("agent:session:chat")
    @ApiOperation(value = "打开会话：复用最近非调试会话，没有才新建")
    public Result<SessionVO> open(@NotNull(message = "智能体ID不能为空") Long agentId) {
        return Result.ok(sessionService.openMine(agentId));
    }

    @Get
    @Mapping("/{id}")
    @SaCheckPermission(value = {"agent:session:query", "agent:session:chat"}, mode = SaMode.OR)
    @ApiOperation(value = "查询会话详情")
    public Result<SessionVO> detail(@NotNull(message = "ID不能为空") Long id) {
        return Result.ok(sessionService.getById(id));
    }

    @Post
    @Mapping("/add")
    @OperLog(title = "调试会话", businessType = BusinessType.INSERT)
    @SaCheckPermission("agent:session:chat")
    @ApiOperation(value = "新建调试会话")
    public Result<SessionVO> add(@Body @Validated SessionSaveDTO dto) {
        return Result.ok(sessionService.create(dto));
    }

    @Delete
    @Mapping("/remove")
    @OperLog(title = "会话记录", businessType = BusinessType.DELETE)
    @SaCheckPermission("agent:session:remove")
    @ApiOperation(value = "按用户消息 ID 删除会话记录")
    public Result<Void> remove(@NotEmpty(message = "请选择要删除的数据") List<Long> ids) {
        sessionService.removeByUserMessageIds(ids);
        return Result.ok();
    }

    @Delete
    @Mapping("/remove-by-ids")
    @OperLog(title = "删除会话", businessType = BusinessType.DELETE)
    @SaCheckPermission(value = {"agent:session:remove", "agent:session:chat"}, mode = SaMode.OR)
    @ApiOperation(value = "按会话 ID 删除整段会话")
    public Result<Void> removeByIds(@NotEmpty(message = "请选择要删除的数据") List<Long> ids) {
        sessionService.removeSessions(ids);
        return Result.ok();
    }

    @Get
    @Mapping("/{id}/messages")
    @SaCheckPermission("agent:session:chat")
    @ApiOperation(value = "查询会话消息列表")
    public Result<List<MessageVO>> messages(@NotNull(message = "ID不能为空") Long id) {
        return Result.ok(sessionService.listMessages(id));
    }

    @Post
    @Mapping("/{id}/send")
    @OperLog(title = "对话", businessType = BusinessType.OTHER)
    @SaCheckPermission("agent:session:chat")
    @ApiOperation(value = "发送对话消息")
    public Result<SendMessageResult> send(@NotNull(message = "ID不能为空") Long id, @Body @Validated SendMessageDTO dto) {
        return Result.ok(sessionService.send(id, dto));
    }

    @Post
    @Mapping("/{id}/send-stream")
    @SaCheckPermission("agent:session:chat")
    @ApiOperation(value = "流式发送对话消息（SSE：user / delta / done / error）")
    public SseEmitter sendStream(@NotNull(message = "ID不能为空") Long id, @Body @Validated SendMessageDTO dto) {
        return sessionService.sendStream(id, dto);
    }
}
