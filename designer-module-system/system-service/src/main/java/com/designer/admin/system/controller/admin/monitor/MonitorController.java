package com.designer.admin.system.controller.admin.monitor;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import cn.dev33.satoken.annotation.SaCheckLogin;
import com.designer.admin.common.core.Result;
import com.designer.admin.system.model.vo.monitor.ServerInfoVO;
import com.designer.admin.system.service.monitor.MonitorService;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;

/**
 * 服务监控（首页仪表盘：总览 / 磁盘分区）
 *
 * @author 雨夜终端
 */
@Controller
@Mapping("/admin-api/system/monitor")
@SaCheckLogin
@Api(tags = "服务监控")
public class MonitorController {

    @Inject
    private MonitorService monitorService;

    @Get
    @Mapping("/server")
    @ApiOperation(value = "获取服务器监控信息")
    public Result<ServerInfoVO> getServerInfo() {
        return Result.ok(monitorService.getServerInfo());
    }
}
