package com.designer.admin.module.agent.controller.admin.agent;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.Result;
import com.designer.admin.module.agent.model.dto.agent.AgentQueryDTO;
import com.designer.admin.module.agent.model.vo.agent.AgentVO;
import com.designer.admin.module.agent.service.agent.AgentAgentService;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.validation.annotation.Valid;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 智能体商店。
 *
 * @author 雨夜终端
 */
@Valid
@Controller
@Mapping("/admin-api/agent/store")
@Api(tags = "智能体商店")
public class AgentStoreController {

    @Inject
    private AgentAgentService agentService;

    @Get
    @Mapping("/page")
    @SaCheckPermission("agent:store:list")
    @ApiOperation(value = "分页查询智能体商店（已启用）")
    public Result<PageResult<AgentVO>> page(AgentQueryDTO query) {
        return Result.ok(PageResult.of(agentService.pageStore(query)));
    }
}
