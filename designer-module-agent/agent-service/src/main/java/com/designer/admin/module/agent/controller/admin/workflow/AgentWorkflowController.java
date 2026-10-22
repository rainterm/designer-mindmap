package com.designer.admin.module.agent.controller.admin.workflow;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.Result;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.module.agent.model.dto.workflow.WorkflowHistoryQueryDTO;
import com.designer.admin.module.agent.model.dto.workflow.WorkflowQueryDTO;
import com.designer.admin.module.agent.model.dto.workflow.WorkflowRollbackDTO;
import com.designer.admin.module.agent.model.dto.workflow.WorkflowRunDTO;
import com.designer.admin.module.agent.model.dto.workflow.WorkflowSaveDTO;
import com.designer.admin.module.agent.model.vo.workflow.WorkflowRunResult;
import com.designer.admin.module.agent.model.vo.workflow.WorkflowVO;
import com.designer.admin.module.agent.model.vo.workflow.WorkflowVersionVO;
import com.designer.admin.module.agent.service.workflow.AgentWorkflowService;
import com.designer.admin.module.agent.workflow.AgentWorkflowExecutor;
import org.noear.solon.annotation.Body;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Delete;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Post;
import org.noear.solon.annotation.Put;
import org.noear.solon.validation.annotation.Valid;
import org.noear.solon.web.sse.SseEmitter;

import java.util.List;
import java.util.Map;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.noear.solon.validation.annotation.Validated;
import org.noear.solon.validation.annotation.NotNull;
import org.noear.solon.validation.annotation.NotEmpty;
import org.noear.solon.validation.annotation.Min;

/**
 * 工作流。
 *
 * @author 雨夜终端
 */
@Valid
@Controller
@Mapping("/admin-api/agent/workflow")
@Api(tags = "工作流")
public class AgentWorkflowController {

    @Inject
    private AgentWorkflowService workflowService;
    @Inject
    private AgentWorkflowExecutor workflowExecutor;

    @Get
    @Mapping("/page")
    @SaCheckPermission("agent:workflow:list")
    @ApiOperation(value = "分页查询工作流")
    public Result<PageResult<WorkflowVO>> page(WorkflowQueryDTO query) {
        return Result.ok(PageResult.of(workflowService.page(query)));
    }

    @Get
    @Mapping("/list")
    @SaCheckPermission("agent:workflow:list")
    @ApiOperation(value = "查询已启用工作流列表")
    public Result<List<WorkflowVO>> list() {
        return Result.ok(workflowService.listEnabled());
    }

    @Get
    @Mapping("/{id}")
    @SaCheckPermission("agent:workflow:query")
    @ApiOperation(value = "查询工作流详情")
    public Result<WorkflowVO> detail(@NotNull(message = "ID不能为空") Long id) {
        return Result.ok(workflowService.getById(id));
    }

    @Post
    @Mapping("/add")
    @OperLog(title = "工作流", businessType = BusinessType.INSERT)
    @SaCheckPermission("agent:workflow:add")
    @ApiOperation(value = "新增工作流")
    public Result<WorkflowVO> add(@Body @Validated WorkflowSaveDTO dto) {
        return Result.ok(workflowService.save(dto));
    }

    @Put
    @Mapping("/update")
    @OperLog(title = "工作流", businessType = BusinessType.UPDATE)
    @SaCheckPermission("agent:workflow:edit")
    @ApiOperation(value = "修改工作流")
    public Result<WorkflowVO> update(@Body @Validated WorkflowSaveDTO dto) {
        return Result.ok(workflowService.update(dto));
    }

    @Delete
    @Mapping("/remove")
    @OperLog(title = "工作流", businessType = BusinessType.DELETE)
    @SaCheckPermission("agent:workflow:remove")
    @ApiOperation(value = "删除工作流")
    public Result<Void> remove(@NotEmpty(message = "请选择要删除的数据") List<Long> ids) {
        workflowService.remove(ids);
        return Result.ok();
    }

    @Post
    @Mapping("/{id}/run")
    @OperLog(title = "工作流试跑", businessType = BusinessType.OTHER)
    @SaCheckPermission("agent:workflow:edit")
    @ApiOperation(value = "试跑工作流（同步整包结果）")
    public Result<WorkflowRunResult> run(@NotNull(message = "ID不能为空") Long id, @Body @Validated WorkflowRunDTO dto) {
        Long agentId = dto == null ? null : dto.getAgentId();
        Map<String, Object> params = dto == null ? null : dto.getParams();
        return Result.ok(workflowExecutor.run(id, params, agentId));
    }

    @Post
    @Mapping("/{id}/run-stream")
    @OperLog(title = "工作流试跑", businessType = BusinessType.OTHER)
    @SaCheckPermission("agent:workflow:edit")
    @ApiOperation(value = "试跑工作流（SSE：status / step / delta / done / error）")
    public SseEmitter runStream(@NotNull(message = "ID不能为空") Long id, @Body @Validated WorkflowRunDTO dto) {
        Long agentId = dto == null ? null : dto.getAgentId();
        Map<String, Object> params = dto == null ? null : dto.getParams();
        return workflowService.runStream(id, params, agentId);
    }

    @Put
    @Mapping("/{id}/publish")
    @OperLog(title = "工作流发布", businessType = BusinessType.UPDATE)
    @SaCheckPermission("agent:workflow:publish")
    @ApiOperation(value = "发布工作流（版本号+1，写历史快照）")
    public Result<WorkflowVO> publish(@NotNull(message = "ID不能为空") Long id) {
        return Result.ok(workflowService.publish(id));
    }

    @Get
    @Mapping("/{id}/history")
    @SaCheckPermission("agent:workflow:history")
    @ApiOperation(value = "分页查询工作流历史版本")
    public Result<PageResult<WorkflowVersionVO>> history(@NotNull(message = "ID不能为空") Long id, WorkflowHistoryQueryDTO query) {
        return Result.ok(PageResult.of(workflowService.listHistory(id, query.getPageNum(), query.getPageSize())));
    }

    @Get
    @Mapping("/{id}/versions/{version}")
    @SaCheckPermission("agent:workflow:history")
    @ApiOperation(value = "查询工作流历史版本详情（含完整 nodes/edges）")
    public Result<WorkflowVersionVO> versionDetail(@NotNull(message = "ID不能为空") Long id, @NotNull(message = "版本号不能为空") @Min(value = 1, message = "版本号无效") Integer version) {
        return Result.ok(workflowService.getVersion(id, version));
    }

    @Post
    @Mapping("/{id}/rollback")
    @OperLog(title = "工作流回滚", businessType = BusinessType.UPDATE)
    @SaCheckPermission("agent:workflow:rollback")
    @ApiOperation(value = "回滚到指定版本（写回草稿，version 不变）")
    public Result<WorkflowVO> rollback(@NotNull(message = "ID不能为空") Long id, @Body @Validated WorkflowRollbackDTO dto) {
        return Result.ok(workflowService.rollback(id, dto == null ? null : dto.getVersion()));
    }
}
