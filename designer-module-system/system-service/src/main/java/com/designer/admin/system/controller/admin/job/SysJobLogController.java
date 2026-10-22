package com.designer.admin.system.controller.admin.job;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.Result;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.system.model.dto.job.JobLogQueryDTO;
import com.designer.admin.system.model.entity.job.SysJobLog;
import com.designer.admin.system.service.job.SysJobLogService;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Delete;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.validation.annotation.Valid;

import java.util.List;

/**
 * 定时任务调度日志
 *
 * @author 雨夜终端
 */
@Valid
@Controller
@Mapping("/admin-api/system/joblog")
@Api(tags = "任务日志")
public class SysJobLogController {

    @Inject
    private SysJobLogService jobLogService;

    @Get
    @Mapping("/page")
    @SaCheckPermission("system:joblog:list")
    @ApiOperation(value = "分页查询任务日志")
    public Result<PageResult<SysJobLog>> listPage(JobLogQueryDTO query) {
        return Result.ok(PageResult.of(jobLogService.listPage(query)));
    }

    @Delete
    @Mapping("/remove")
    @OperLog(title = "调度日志", businessType = BusinessType.DELETE)
    @SaCheckPermission("system:joblog:remove")
    @ApiOperation(value = "删除任务日志")
    public Result<Void> remove(List<Long> ids) {
        jobLogService.removeByIds(ids);
        return Result.ok();
    }

    @Delete
    @Mapping("/clear")
    @OperLog(title = "调度日志", businessType = BusinessType.CLEAN)
    @SaCheckPermission("system:joblog:remove")
    @ApiOperation(value = "清空任务日志")
    public Result<Void> removeAll() {
        jobLogService.removeAll();
        return Result.ok();
    }

    @Delete
    @Mapping("/clear/{jobId}")
    @OperLog(title = "调度日志", businessType = BusinessType.CLEAN)
    @SaCheckPermission("system:joblog:remove")
    @ApiOperation(value = "根据任务删除日志")
    public Result<Void> removeByJobId(Long jobId) {
        jobLogService.removeByJobId(jobId);
        return Result.ok();
    }
}
