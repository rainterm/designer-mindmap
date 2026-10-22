package com.designer.admin.module.agent.controller.app;

import cn.dev33.satoken.annotation.SaIgnore;
import com.designer.admin.common.core.Result;
import com.designer.admin.framework.satoken.OperatorHelper;
import com.designer.admin.module.agent.model.dto.session.SendMessageDTO;
import com.designer.admin.module.agent.model.dto.session.SessionSaveDTO;
import com.designer.admin.module.agent.model.entity.agent.AgentAgent;
import com.designer.admin.module.agent.model.vo.agent.AgentPublicVO;
import com.designer.admin.module.agent.model.vo.session.MessageVO;
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
import org.noear.solon.core.handle.Context;
import org.noear.solon.validation.annotation.NotBlank;
import org.noear.solon.validation.annotation.NotEmpty;
import org.noear.solon.validation.annotation.NotNull;
import org.noear.solon.validation.annotation.Valid;
import org.noear.solon.validation.annotation.Validated;
import org.noear.solon.web.sse.SseEmitter;

import java.util.List;

/**
 * 智能体公开访问接口（免登录，靠 public_token + 可选访客头）。
 *
 * @author 雨夜终端
 */
@SaIgnore
@Valid
@Controller
@Mapping("/app-api/agent/public")
@Api(tags = "公开访问-智能体")
public class AppAgentPublicController {

    /** 浏览器访客隔离头，对应 localStorage 中的 visitorId */
    public static final String HEADER_VISITOR = "X-Public-Visitor";

    @Inject
    private AgentAgentService agentService;
    @Inject
    private AgentSessionService sessionService;

    @Get
    @Mapping("/{token}")
    @ApiOperation(value = "公开智能体信息")
    public Result<AgentPublicVO> info(@NotBlank(message = "token不能为空") String token) {
        return Result.ok(toPublicVo(agentService.requireByPublicToken(token)));
    }

    @Get
    @Mapping("/{token}/sessions")
    @ApiOperation(value = "当前访客的会话列表")
    public Result<List<SessionVO>> listSessions(@NotBlank(message = "token不能为空") String token) {
        AgentAgent agent = agentService.requireByPublicToken(token);
        return Result.ok(sessionService.listByOwner(agent.getId(), owner(token)));
    }

    @Post
    @Mapping("/{token}/session")
    @ApiOperation(value = "打开会话：复用最近会话，没有才新建")
    public Result<SessionVO> openSession(@NotBlank(message = "token不能为空") String token) {
        AgentAgent agent = agentService.requireByPublicToken(token);
        return Result.ok(sessionService.openByOwner(agent.getId(), owner(token)));
    }

    @Post
    @Mapping("/{token}/sessions")
    @ApiOperation(value = "新建会话")
    public Result<SessionVO> createSession(@NotBlank(message = "token不能为空") String token,
                                           @Body SessionSaveDTO dto) {
        AgentAgent agent = agentService.requireByPublicToken(token);
        SessionSaveDTO body = dto == null ? new SessionSaveDTO() : dto;
        body.setAgentId(agent.getId());
        if (body.getTitle() == null || body.getTitle().isBlank()) {
            body.setTitle("新会话");
        }
        return Result.ok(sessionService.create(body, owner(token)));
    }

    @Delete
    @Mapping("/{token}/sessions")
    @ApiOperation(value = "删除会话")
    public Result<Void> removeSessions(@NotBlank(message = "token不能为空") String token,
                                       @NotEmpty(message = "请选择要删除的数据") List<Long> ids) {
        agentService.requireByPublicToken(token);
        sessionService.removeSessionsByOwner(ids, owner(token));
        return Result.ok();
    }

    @Get
    @Mapping("/{token}/sessions/{sessionId}/messages")
    @ApiOperation(value = "查询访客会话消息")
    public Result<List<MessageVO>> messages(@NotBlank(message = "token不能为空") String token,
                                            @NotNull(message = "会话ID不能为空") Long sessionId) {
        agentService.requireByPublicToken(token);
        sessionService.requireOwned(sessionId, owner(token));
        return Result.ok(sessionService.listMessages(sessionId));
    }

    @Post
    @Mapping("/{token}/sessions/{sessionId}/send-stream")
    @ApiOperation(value = "访客流式对话（SSE，非 Result 包装）")
    public SseEmitter sendStream(@NotBlank(message = "token不能为空") String token,
                                 @NotNull(message = "会话ID不能为空") Long sessionId,
                                 @Body @Validated SendMessageDTO dto) {
        agentService.requireByPublicToken(token);
        sessionService.requireOwned(sessionId, owner(token));
        SendMessageDTO body = dto == null ? new SendMessageDTO() : dto;
        body.setUseDraft(false);
        return sessionService.sendStream(sessionId, body);
    }

    private static String owner(String token) {
        Context ctx = Context.current();
        String visitor = ctx == null ? null : ctx.header(HEADER_VISITOR);
        return OperatorHelper.publicOperator(token, visitor);
    }

    private static AgentPublicVO toPublicVo(AgentAgent agent) {
        AgentPublicVO vo = new AgentPublicVO();
        vo.setName(agent.getName());
        vo.setAvatar(agent.getAvatar());
        vo.setRemark(agent.getRemark());
        vo.setFileEnabled(agent.getFileEnabled() == null ? 0 : agent.getFileEnabled());
        return vo;
    }
}
