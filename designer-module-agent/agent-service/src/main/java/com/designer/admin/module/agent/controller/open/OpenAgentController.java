package com.designer.admin.module.agent.controller.open;

import com.designer.admin.common.core.Result;
import com.designer.admin.module.agent.model.vo.agent.AgentVO;
import com.designer.admin.module.agent.service.agent.AgentAgentService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.validation.annotation.NotNull;
import org.noear.solon.validation.annotation.Valid;

import java.util.List;

/**
 * 开放接口：智能体查询（OAuth2 + Scope open.agent）。
 *
 * @author 雨夜终端
 */
@Valid
@Controller
@Mapping("/open-api/agent/agent")
@Api(tags = "开放接口-智能体")
public class OpenAgentController {

    @Inject
    private AgentAgentService agentService;

    @Get
    @Mapping("/list")
    @ApiOperation(value = "已发布智能体列表")
    public Result<List<AgentVO>> list() {
        return Result.ok(agentService.listPublished().stream().map(this::forOpen).toList());
    }

    @Get
    @Mapping("/{id}")
    @ApiOperation(value = "已发布智能体详情")
    public Result<AgentVO> detail(@NotNull(message = "ID不能为空") Long id) {
        return Result.ok(forOpen(agentService.getPublishedById(id)));
    }

    /** 开放口不返回公开 token */
    private AgentVO forOpen(AgentVO vo) {
        if (vo != null) {
            vo.setPublicToken(null);
        }
        return vo;
    }
}
