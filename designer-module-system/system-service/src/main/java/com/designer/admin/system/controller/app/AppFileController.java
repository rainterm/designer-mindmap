package com.designer.admin.system.controller.app;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import cn.dev33.satoken.annotation.SaIgnore;
import com.designer.admin.system.service.file.SysFileService;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.core.handle.Context;

/**
 * 文件公开下载（匿名）：只放行存储配置标记为「公开」的文件。
 *
 * @author 雨夜终端
 */
@SaIgnore
@Controller
@Mapping("/app-api/system/file")
@Api(tags = "文件下载")
public class AppFileController {

    @Inject
    private SysFileService fileService;

    @Get
    @Mapping("/download/**")
    @ApiOperation(value = "公开文件下载")
    public void download(Context ctx) throws Exception {
        fileService.download(ctx, true);
    }
}
