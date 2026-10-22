package com.designer.admin.module.agent.controller.open;

import com.designer.admin.common.core.Result;
import com.designer.admin.framework.satoken.OperatorHelper;
import com.designer.admin.module.agent.model.dto.session.SendMessageDTO;
import com.designer.admin.module.agent.model.dto.session.SessionSaveDTO;
import com.designer.admin.module.agent.model.vo.session.MessageVO;
import com.designer.admin.module.agent.model.vo.session.SendMessageResult;
import com.designer.admin.module.agent.model.vo.session.SessionVO;
import com.designer.admin.module.agent.service.agent.AgentAgentService;
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
import org.noear.solon.validation.annotation.NotEmpty;
import org.noear.solon.validation.annotation.NotNull;
import org.noear.solon.validation.annotation.Valid;
import org.noear.solon.validation.annotation.Validated;
import org.noear.solon.web.sse.SseEmitter;

import java.util.List;

/**
 * 开放接口：智能体对话（OAuth2 + Scope open.agent.chat）。
 *
 * @author 雨夜终端
 */
@Valid
@Controller
@Mapping("/open-api/agent/session")
@Api(tags = "开放接口-智能体对话")
public class OpenAgentSessionController {

    @Inject
    private AgentSessionService sessionService;
    @Inject
    private AgentAgentService agentService;

    @Get
    @Mapping("/list")
    @ApiOperation(value = "当前客户端在指定智能体下的会话列表")
    public Result<List<SessionVO>> list(@NotNull(message = "智能体ID不能为空") Long agentId) {
        agentService.getPublishedById(agentId);
        return Result.ok(sessionService.listByOwner(agentId, requireOwner()));
    }

    @Get
    @Mapping("/open")
    @ApiOperation(value = "打开会话：复用最近会话，没有才新建")
    public Result<SessionVO> open(@NotNull(message = "智能体ID不能为空") Long agentId) {
        agentService.getPublishedById(agentId);
        return Result.ok(sessionService.openByOwner(agentId, requireOwner()));
    }

    @Get
    @Mapping("/{id}")
    @ApiOperation(value = "查询会话详情")
    public Result<SessionVO> detail(@NotNull(message = "ID不能为空") Long id) {
        return Result.ok(sessionService.requireOwned(id, requireOwner()));
    }

    @Post
    @Mapping("/add")
    @ApiOperation(value = "新建会话")
    public Result<SessionVO> add(@Body @Validated SessionSaveDTO dto) {
        agentService.getPublishedById(dto.getAgentId());
        return Result.ok(sessionService.create(dto, requireOwner()));
    }

    @Delete
    @Mapping("/remove-by-ids")
    @ApiOperation(value = "按会话 ID 删除")
    public Result<Void> removeByIds(@NotEmpty(message = "请选择要删除的数据") List<Long> ids) {
        // removeSessions 内部按当前 OperatorHelper 过滤归属
        sessionService.removeSessions(ids);
        return Result.ok();
    }

    @Get
    @Mapping("/{id}/messages")
    @ApiOperation(value = "查询会话消息列表")
    public Result<List<MessageVO>> messages(@NotNull(message = "ID不能为空") Long id) {
        sessionService.requireOwned(id, requireOwner());
        return Result.ok(sessionService.listMessages(id));
    }

    @Post
    @Mapping("/{id}/send")
    @ApiOperation(value = "发送对话消息（同步）")
    public Result<SendMessageResult> send(@NotNull(message = "ID不能为空") Long id,
                                          @Body @Validated SendMessageDTO dto) {
        sessionService.requireOwned(id, requireOwner());
        return Result.ok(sessionService.send(id, publishedOnly(dto)));
    }

    @Post
    @Mapping("/{id}/send-stream")
    @ApiOperation(value = "流式发送对话消息（SSE：user / delta / done / error，非 Result 包装）")
    public SseEmitter sendStream(@NotNull(message = "ID不能为空") Long id,
                                 @Body @Validated SendMessageDTO dto) {
        sessionService.requireOwned(id, requireOwner());
        return sessionService.sendStream(id, publishedOnly(dto));
    }

    private static String requireOwner() {
        String owner = OperatorHelper.currentOperator();
        if (owner == null || owner.isBlank()) {
            throw new com.designer.admin.common.exception.BizException(401, "未授权或 access_token 无效");
        }
        return owner;
    }

    private static SendMessageDTO publishedOnly(SendMessageDTO dto) {
        SendMessageDTO body = dto == null ? new SendMessageDTO() : dto;
        body.setUseDraft(false);
        return body;
    }
}
