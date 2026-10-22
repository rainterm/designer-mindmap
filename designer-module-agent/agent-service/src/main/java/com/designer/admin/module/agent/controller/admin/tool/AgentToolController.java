package com.designer.admin.module.agent.controller.admin.tool;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.Result;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.module.agent.model.dto.tool.ToolQueryDTO;
import com.designer.admin.module.agent.model.dto.tool.ToolSaveDTO;
import com.designer.admin.module.agent.model.vo.tool.ToolVO;
import com.designer.admin.module.agent.service.tool.AgentToolService;
import org.noear.solon.annotation.Body;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Delete;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Post;
import org.noear.solon.annotation.Put;
import org.noear.solon.validation.annotation.Valid;

import java.util.List;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.noear.solon.validation.annotation.Validated;
import org.noear.solon.validation.annotation.NotEmpty;

/**
 * 智能体工具。
 *
 * @author 雨夜终端
 */
@Valid
@Controller
@Mapping("/admin-api/agent/tool")
@Api(tags = "智能体工具")
public class AgentToolController {

    @Inject
    private AgentToolService toolService;

    @Get
    @Mapping("/page")
    @SaCheckPermission("agent:tool:list")
    @ApiOperation(value = "分页查询平台工具")
    public Result<PageResult<ToolVO>> page(ToolQueryDTO query) {
        return Result.ok(PageResult.of(toolService.page(query)));
    }

    @Get
    @Mapping("/list")
    @SaCheckPermission("agent:tool:list")
    @ApiOperation(value = "查询已启用工具列表")
    public Result<List<ToolVO>> list() {
        return Result.ok(toolService.listEnabled());
    }

    @Post
    @Mapping("/add")
    @OperLog(title = "平台工具", businessType = BusinessType.INSERT)
    @SaCheckPermission("agent:tool:add")
    @ApiOperation(value = "新增平台工具（内置工具不可新增）")
    public Result<Void> add() {
        toolService.create();
        return Result.ok();
    }

    @Put
    @Mapping("/update")
    @OperLog(title = "平台工具", businessType = BusinessType.UPDATE)
    @SaCheckPermission("agent:tool:edit")
    @ApiOperation(value = "修改平台工具（仅允许改状态）")
    public Result<ToolVO> update(@Body @Validated ToolSaveDTO dto) {
        return Result.ok(toolService.update(dto));
    }

    @Delete
    @Mapping("/remove")
    @OperLog(title = "平台工具", businessType = BusinessType.DELETE)
    @SaCheckPermission("agent:tool:remove")
    @ApiOperation(value = "删除平台工具（内置工具不可删除）")
    public Result<Void> remove(@NotEmpty(message = "请选择要删除的数据") List<Long> ids) {
        toolService.remove(ids);
        return Result.ok();
    }
}
