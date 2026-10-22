package com.designer.admin.system.controller.admin.file;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.Result;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.system.model.dto.file.FileQueryDTO;
import com.designer.admin.system.model.entity.file.SysFile;
import com.designer.admin.system.service.file.SysFileService;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Delete;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.validation.annotation.Valid;

import java.util.List;

/**
 * 文件管理：分页查询、详情、删除
 *
 * @author 雨夜终端
 */
@Valid
@Controller
@Mapping("/admin-api/system/file")
@Api(tags = "文件管理")
public class SysFileController {

    @Inject
    private SysFileService fileService;

    @Get
    @Mapping("/page")
    @SaCheckPermission("system:file:list")
    @ApiOperation(value = "分页查询文件")
    public Result<PageResult<SysFile>> listPage(FileQueryDTO query) {
        return Result.ok(PageResult.of(fileService.listPage(query)));
    }

    @Get
    @Mapping("/{id}")
    @SaCheckPermission("system:file:query")
    @ApiOperation(value = "获取文件详情")
    public Result<SysFile> getDetail(Long id) {
        return Result.ok(fileService.getById(id));
    }

    @Delete
    @Mapping("/remove")
    @OperLog(title = "文件管理", businessType = BusinessType.DELETE)
    @SaCheckPermission("system:file:remove")
    @ApiOperation(value = "删除文件")
    public Result<Void> remove(List<Long> ids) {
        fileService.removeByIds(ids);
        return Result.ok();
    }
}
