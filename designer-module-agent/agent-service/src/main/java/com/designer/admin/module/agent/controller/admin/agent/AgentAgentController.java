package com.designer.admin.module.agent.controller.admin.agent;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.Result;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.module.agent.model.dto.agent.AgentHistoryQueryDTO;
import com.designer.admin.module.agent.model.dto.agent.AgentQueryDTO;
import com.designer.admin.module.agent.model.dto.agent.AgentRollbackDTO;
import com.designer.admin.module.agent.model.dto.agent.AgentSaveDTO;
import com.designer.admin.module.agent.model.vo.agent.AgentVO;
import com.designer.admin.module.agent.model.vo.agent.AgentVersionVO;
import com.designer.admin.module.agent.service.agent.AgentAgentService;
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
import org.noear.solon.validation.annotation.NotNull;
import org.noear.solon.validation.annotation.NotEmpty;
import org.noear.solon.validation.annotation.Min;

/**
 * 智能体管理。
 *
 * @author 雨夜终端
 */
@Valid
@Controller
@Mapping("/admin-api/agent/agent")
@Api(tags = "智能体管理")
public class AgentAgentController {

    @Inject
    private AgentAgentService agentService;

    @Get
    @Mapping("/page")
    @SaCheckPermission("agent:agent:list")
    @ApiOperation(value = "分页查询智能体")
    public Result<PageResult<AgentVO>> page(AgentQueryDTO query) {
        return Result.ok(PageResult.of(agentService.page(query)));
    }

    @Get
    @Mapping("/list")
    @SaCheckPermission("agent:agent:list")
    @ApiOperation(value = "查询已启用智能体列表")
    public Result<List<AgentVO>> list() {
        return Result.ok(agentService.listEnabled());
    }

    @Get
    @Mapping("/{id}")
    @SaCheckPermission("agent:agent:query")
    @ApiOperation(value = "查询智能体详情")
    public Result<AgentVO> detail(@NotNull(message = "ID不能为空") Long id) {
        return Result.ok(agentService.getById(id));
    }

    @Post
    @Mapping("/add")
    @OperLog(title = "智能体", businessType = BusinessType.INSERT)
    @SaCheckPermission("agent:agent:add")
    @ApiOperation(value = "新增智能体")
    public Result<AgentVO> add(@Body @Validated AgentSaveDTO dto) {
        return Result.ok(agentService.save(dto));
    }

    @Put
    @Mapping("/update")
    @OperLog(title = "智能体", businessType = BusinessType.UPDATE)
    @SaCheckPermission("agent:agent:edit")
    @ApiOperation(value = "修改智能体")
    public Result<AgentVO> update(@Body @Validated AgentSaveDTO dto) {
        return Result.ok(agentService.update(dto));
    }

    @Delete
    @Mapping("/remove")
    @OperLog(title = "智能体", businessType = BusinessType.DELETE)
    @SaCheckPermission("agent:agent:remove")
    @ApiOperation(value = "删除智能体")
    public Result<Void> remove(@NotEmpty(message = "请选择要删除的数据") List<Long> ids) {
        agentService.remove(ids);
        return Result.ok();
    }

    @Put
    @Mapping("/{id}/publish")
    @OperLog(title = "智能体发布", businessType = BusinessType.UPDATE)
    @SaCheckPermission("agent:agent:publish")
    @ApiOperation(value = "发布智能体（草稿固化，版本号+1，写历史快照）")
    public Result<AgentVO> publish(@NotNull(message = "ID不能为空") Long id) {
        return Result.ok(agentService.publish(id));
    }

    @Get
    @Mapping("/{id}/history")
    @SaCheckPermission("agent:agent:history")
    @ApiOperation(value = "分页查询智能体历史版本")
    public Result<PageResult<AgentVersionVO>> history(@NotNull(message = "ID不能为空") Long id, AgentHistoryQueryDTO query) {
        return Result.ok(PageResult.of(agentService.listHistory(id, query.getPageNum(), query.getPageSize())));
    }

    @Get
    @Mapping("/{id}/versions/{version}")
    @SaCheckPermission("agent:agent:history")
    @ApiOperation(value = "查询智能体历史版本详情（含完整配置）")
    public Result<AgentVersionVO> versionDetail(@NotNull(message = "ID不能为空") Long id, @NotNull(message = "版本号不能为空") @Min(value = 1, message = "版本号无效") Integer version) {
        return Result.ok(agentService.getVersion(id, version));
    }

    @Post
    @Mapping("/{id}/rollback")
    @OperLog(title = "智能体回滚", businessType = BusinessType.UPDATE)
    @SaCheckPermission("agent:agent:rollback")
    @ApiOperation(value = "回滚到指定版本（覆盖草稿，version 不变）")
    public Result<AgentVO> rollback(@NotNull(message = "ID不能为空") Long id, @Body @Validated AgentRollbackDTO dto) {
        return Result.ok(agentService.rollback(id, dto == null ? null : dto.getVersion()));
    }

    @Put
    @Mapping("/{id}/public/enable")
    @OperLog(title = "智能体公开链接", businessType = BusinessType.UPDATE)
    @SaCheckPermission("agent:agent:publish")
    @ApiOperation(value = "开启公开访问链接（需已发布）")
    public Result<AgentVO> enablePublic(@NotNull(message = "ID不能为空") Long id) {
        return Result.ok(agentService.enablePublic(id));
    }

    @Put
    @Mapping("/{id}/public/disable")
    @OperLog(title = "智能体公开链接", businessType = BusinessType.UPDATE)
    @SaCheckPermission("agent:agent:publish")
    @ApiOperation(value = "关闭公开访问链接")
    public Result<AgentVO> disablePublic(@NotNull(message = "ID不能为空") Long id) {
        return Result.ok(agentService.disablePublic(id));
    }

    @Post
    @Mapping("/{id}/public/rotate")
    @OperLog(title = "智能体公开链接", businessType = BusinessType.UPDATE)
    @SaCheckPermission("agent:agent:publish")
    @ApiOperation(value = "轮换公开访问 token（旧链接失效）")
    public Result<AgentVO> rotatePublic(@NotNull(message = "ID不能为空") Long id) {
        return Result.ok(agentService.rotatePublic(id));
    }
}
