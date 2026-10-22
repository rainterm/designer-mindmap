package com.designer.admin.system.controller.admin.monitor;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import cn.dev33.satoken.annotation.SaIgnore;
import com.designer.admin.common.core.Result;
import com.designer.admin.system.constant.SysMonitorConsts;
import com.designer.admin.system.model.vo.monitor.HealthVO;
import org.noear.solon.Solon;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Mapping;

/**
 * 健康检查
 *
 * @author 雨夜终端
 */
@Controller
@Mapping("/admin-api/system")
@Api(tags = "健康检查")
public class HealthController {

    @SaIgnore
    @Get
    @Mapping("/health")
    @ApiOperation(value = "健康检查")
    public Result<HealthVO> health() {
        return Result.ok(HealthVO.builder()
                .app(SysMonitorConsts.HEALTH_APP_NAME)
                .solon(Solon.version())
                .status(SysMonitorConsts.HEALTH_STATUS_UP)
                .build());
    }
}
