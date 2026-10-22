package com.designer.admin.system.controller.admin.log;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.Result;
import com.designer.admin.system.model.dto.log.LoginLogQueryDTO;
import com.designer.admin.system.model.dto.log.OperLogQueryDTO;
import com.designer.admin.system.model.entity.log.SysLoginLog;
import com.designer.admin.system.model.entity.log.SysOperLog;
import com.designer.admin.system.service.log.SysLoginLogService;
import com.designer.admin.system.service.log.SysOperLogService;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.validation.annotation.Valid;

/**
 * 日志管理（操作日志 + 登录日志，仅查询；清理由定时任务负责）
 *
 * @author 雨夜终端
 */
@Valid
@Controller
@Mapping("/admin-api/system/log")
@Api(tags = "日志查询")
public class SysLogController {

    @Inject
    private SysOperLogService operLogService;

    @Inject
    private SysLoginLogService loginLogService;

    @Get
    @Mapping("/oper/page")
    @SaCheckPermission("system:operlog:list")
    @ApiOperation(value = "分页查询操作日志")
    public Result<PageResult<SysOperLog>> listOperLogPage(OperLogQueryDTO query) {
        return Result.ok(PageResult.of(operLogService.listPage(query)));
    }

    @Get
    @Mapping("/login/page")
    @SaCheckPermission("system:loginlog:list")
    @ApiOperation(value = "分页查询登录日志")
    public Result<PageResult<SysLoginLog>> listLoginLogPage(LoginLogQueryDTO query) {
        return Result.ok(PageResult.of(loginLogService.listPage(query)));
    }
}
