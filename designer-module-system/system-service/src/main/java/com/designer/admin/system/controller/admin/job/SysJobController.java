package com.designer.admin.system.controller.admin.job;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.Result;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.system.model.dto.job.JobQueryDTO;
import com.designer.admin.system.model.dto.job.JobSaveDTO;
import com.designer.admin.system.model.entity.job.SysJob;
import com.designer.admin.system.model.vo.job.JobDetailVO;
import com.designer.admin.system.service.job.SysJobService;
import org.noear.solon.annotation.Body;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Delete;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Post;
import org.noear.solon.annotation.Put;
import org.noear.solon.validation.annotation.Valid;
import org.noear.solon.validation.annotation.Validated;

import java.util.List;

/**
 * 定时任务管理
 *
 * @author 雨夜终端
 */
@Valid
@Controller
@Mapping("/admin-api/system/job")
@Api(tags = "定时任务")
public class SysJobController {

    @Inject
    private SysJobService jobService;

    @Get
    @Mapping("/page")
    @SaCheckPermission("system:job:list")
    @ApiOperation(value = "分页查询定时任务")
    public Result<PageResult<SysJob>> listPage(JobQueryDTO query) {
        return Result.ok(PageResult.of(jobService.listPage(query)));
    }

    @Get
    @Mapping("/{id}")
    @SaCheckPermission("system:job:query")
    @ApiOperation(value = "获取定时任务详情")
    public Result<JobDetailVO> getDetail(Long id) {
        return Result.ok(jobService.getDetailView(id));
    }

    @Post
    @Mapping("/add")
    @OperLog(title = "定时任务", businessType = BusinessType.INSERT)
    @SaCheckPermission("system:job:add")
    @ApiOperation(value = "新增定时任务")
    public Result<Void> add(@Body @Validated JobSaveDTO dto) {
        jobService.add(dto);
        return Result.ok();
    }

    @Put
    @Mapping("/update")
    @OperLog(title = "定时任务", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:job:edit")
    @ApiOperation(value = "修改定时任务")
    public Result<Void> update(@Body @Validated JobSaveDTO dto) {
        jobService.update(dto);
        return Result.ok();
    }

    @Delete
    @Mapping("/remove")
    @OperLog(title = "定时任务", businessType = BusinessType.DELETE)
    @SaCheckPermission("system:job:remove")
    @ApiOperation(value = "删除定时任务")
    public Result<Void> remove(List<Long> ids) {
        jobService.removeByIds(ids);
        return Result.ok();
    }

    @Put
    @Mapping("/{id}/status")
    @OperLog(title = "定时任务", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:job:edit")
    @ApiOperation(value = "修改定时任务状态")
    public Result<Void> updateStatus(Long id, Integer status) {
        jobService.updateStatus(id, status);
        return Result.ok();
    }

    @Put
    @Mapping("/{id}/run")
    @OperLog(title = "定时任务", businessType = BusinessType.OTHER)
    @SaCheckPermission("system:job:run")
    @ApiOperation(value = "立即执行定时任务")
    public Result<Void> run(Long id) {
        jobService.runOnce(id);
        return Result.ok();
    }
}
