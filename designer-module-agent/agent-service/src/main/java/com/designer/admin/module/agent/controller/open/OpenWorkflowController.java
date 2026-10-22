package com.designer.admin.module.agent.controller.open;

import com.designer.admin.common.core.Result;
import com.designer.admin.module.agent.model.dto.workflow.WorkflowRunDTO;
import com.designer.admin.module.agent.model.vo.workflow.WorkflowRunResult;
import com.designer.admin.module.agent.model.vo.workflow.WorkflowVO;
import com.designer.admin.module.agent.service.workflow.AgentWorkflowService;
import com.designer.admin.module.agent.workflow.AgentWorkflowExecutor;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.noear.solon.annotation.Body;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Post;
import org.noear.solon.validation.annotation.NotNull;
import org.noear.solon.validation.annotation.Valid;
import org.noear.solon.validation.annotation.Validated;
import org.noear.solon.web.sse.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * 开放接口：工作流查询与执行（OAuth2 + Scope open.workflow）。
 *
 * @author 雨夜终端
 */
@Valid
@Controller
@Mapping("/open-api/agent/workflow")
@Api(tags = "开放接口-工作流")
public class OpenWorkflowController {

    @Inject
    private AgentWorkflowService workflowService;
    @Inject
    private AgentWorkflowExecutor workflowExecutor;

    @Get
    @Mapping("/list")
    @ApiOperation(value = "已发布工作流列表")
    public Result<List<WorkflowVO>> list() {
        return Result.ok(workflowService.listPublished());
    }

    @Get
    @Mapping("/{id}")
    @ApiOperation(value = "已发布工作流详情")
    public Result<WorkflowVO> detail(@NotNull(message = "ID不能为空") Long id) {
        return Result.ok(workflowService.getPublishedById(id));
    }

    @Post
    @Mapping("/{id}/run")
    @ApiOperation(value = "执行工作流（同步整包结果）")
    public Result<WorkflowRunResult> run(@NotNull(message = "ID不能为空") Long id,
                                         @Body @Validated WorkflowRunDTO dto) {
        workflowService.getPublishedById(id);
        Long agentId = dto == null ? null : dto.getAgentId();
        Map<String, Object> params = dto == null ? null : dto.getParams();
        return Result.ok(workflowExecutor.run(id, params, agentId, null, false));
    }

    @Post
    @Mapping("/{id}/run-stream")
    @ApiOperation(value = "执行工作流（SSE：status / step / delta / done / error，非 Result 包装）")
    public SseEmitter runStream(@NotNull(message = "ID不能为空") Long id,
                                @Body @Validated WorkflowRunDTO dto) {
        workflowService.getPublishedById(id);
        Long agentId = dto == null ? null : dto.getAgentId();
        Map<String, Object> params = dto == null ? null : dto.getParams();
        return workflowService.runPublishedStream(id, params, agentId);
    }
}
