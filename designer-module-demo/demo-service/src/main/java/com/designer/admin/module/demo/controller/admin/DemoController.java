package com.designer.admin.module.demo.controller.admin;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import com.designer.admin.common.core.Result;
import com.designer.admin.module.demo.api.model.DemoPingVO;
import com.designer.admin.module.demo.service.DemoService;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;

/**
 * Demo 示例接口。二次开发可复制本模块后改包名与路径。
 *
 * @author 雨夜终端
 */
@Controller
@Mapping("/admin-api/demo")
@Api(tags = "示例接口")
public class DemoController {

    @Inject
    private DemoService demoService;

    @Get
    @Mapping("/ping")
    @ApiOperation(value = "示例连通性测试")
    public Result<DemoPingVO> ping() {
        return Result.ok(demoService.ping());
    }
}
