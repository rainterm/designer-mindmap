package com.designer.admin.module.agent.controller.admin.mcp;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.Result;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.module.agent.model.dto.mcp.McpQueryDTO;
import com.designer.admin.module.agent.model.dto.mcp.McpSaveDTO;
import com.designer.admin.module.agent.model.vo.mcp.McpVO;
import com.designer.admin.module.agent.service.mcp.AgentMcpService;
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
import java.util.Map;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.noear.solon.validation.annotation.Validated;
import org.noear.solon.validation.annotation.NotNull;
import org.noear.solon.validation.annotation.NotEmpty;

/**
 * MCP 管理。
 *
 * @author 雨夜终端
 */
@Valid
@Controller
@Mapping("/admin-api/agent/mcp")
@Api(tags = "MCP 管理")
public class AgentMcpController {

    @Inject
    private AgentMcpService mcpService;

    @Get
    @Mapping("/page")
    @SaCheckPermission("agent:mcp:list")
    @ApiOperation(value = "分页查询 MCP 服务")
    public Result<PageResult<McpVO>> page(McpQueryDTO query) {
        return Result.ok(PageResult.of(mcpService.page(query)));
    }

    @Get
    @Mapping("/list")
    @SaCheckPermission("agent:mcp:list")
    @ApiOperation(value = "查询已启用 MCP 列表")
    public Result<List<McpVO>> list() {
        return Result.ok(mcpService.listEnabled());
    }

    @Post
    @Mapping("/add")
    @OperLog(title = "MCP 服务", businessType = BusinessType.INSERT)
    @SaCheckPermission("agent:mcp:add")
    @ApiOperation(value = "新增 MCP 服务")
    public Result<McpVO> add(@Body @Validated McpSaveDTO dto) {
        return Result.ok(mcpService.save(dto));
    }

    @Put
    @Mapping("/update")
    @OperLog(title = "MCP 服务", businessType = BusinessType.UPDATE)
    @SaCheckPermission("agent:mcp:edit")
    @ApiOperation(value = "修改 MCP 服务")
    public Result<McpVO> update(@Body @Validated McpSaveDTO dto) {
        return Result.ok(mcpService.update(dto));
    }

    @Delete
    @Mapping("/remove")
    @OperLog(title = "MCP 服务", businessType = BusinessType.DELETE)
    @SaCheckPermission("agent:mcp:remove")
    @ApiOperation(value = "删除 MCP 服务")
    public Result<Void> remove(@NotEmpty(message = "请选择要删除的数据") List<Long> ids) {
        mcpService.remove(ids);
        return Result.ok();
    }

    @Post
    @Mapping("/{id}/probe")
    @OperLog(title = "MCP 探测", businessType = BusinessType.UPDATE)
    @SaCheckPermission("agent:mcp:edit")
    @ApiOperation(value = "探测已保存 MCP 的工具清单")
    public Result<McpVO> probe(@NotNull(message = "ID不能为空") Long id) {
        return Result.ok(mcpService.probe(id));
    }

    @Post
    @Mapping("/probe-tools")
    @SaCheckPermission(value = {"agent:mcp:add", "agent:mcp:edit"}, mode = SaMode.OR)
    @ApiOperation(value = "按当前表单配置探测工具清单（不落库）")
    public Result<List<Map<String, Object>>> probeTools(@Body @Validated McpSaveDTO dto) {
        return Result.ok(mcpService.probeDraft(dto));
    }
}
