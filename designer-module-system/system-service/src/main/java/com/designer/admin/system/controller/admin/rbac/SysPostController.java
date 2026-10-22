package com.designer.admin.system.controller.admin.rbac;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.Result;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.system.model.dto.rbac.PostQueryDTO;
import com.designer.admin.system.model.dto.rbac.PostSaveDTO;
import com.designer.admin.system.model.entity.rbac.SysPost;
import com.designer.admin.system.service.rbac.SysPostService;
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
 * 岗位管理：CRUD + 启停
 *
 * @author 雨夜终端
 */
@Valid
@Controller
@Mapping("/admin-api/system/post")
@Api(tags = "岗位管理")
public class SysPostController {

    @Inject
    private SysPostService postService;

    @Get
    @Mapping("/page")
    @SaCheckPermission("system:post:list")
    @ApiOperation(value = "分页查询岗位")
    public Result<PageResult<SysPost>> listPage(PostQueryDTO query) {
        return Result.ok(PageResult.of(postService.listPage(query)));
    }

    @Get
    @Mapping("/options")
    @ApiOperation(value = "查询岗位下拉选项")
    public Result<List<SysPost>> listOptions() {
        return Result.ok(postService.listOptions());
    }

    @Get
    @Mapping("/{id}")
    @SaCheckPermission("system:post:query")
    @ApiOperation(value = "获取岗位详情")
    public Result<SysPost> getDetail(Long id) {
        return Result.ok(postService.getById(id));
    }

    @Post
    @Mapping("/add")
    @OperLog(title = "岗位管理", businessType = BusinessType.INSERT)
    @SaCheckPermission("system:post:add")
    @ApiOperation(value = "新增岗位")
    public Result<Void> add(@Body @Validated PostSaveDTO dto) {
        postService.add(dto);
        return Result.ok();
    }

    @Put
    @Mapping("/update")
    @OperLog(title = "岗位管理", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:post:edit")
    @ApiOperation(value = "修改岗位")
    public Result<Void> update(@Body @Validated PostSaveDTO dto) {
        postService.update(dto);
        return Result.ok();
    }

    @Delete
    @Mapping("/remove")
    @OperLog(title = "岗位管理", businessType = BusinessType.DELETE)
    @SaCheckPermission("system:post:remove")
    @ApiOperation(value = "删除岗位")
    public Result<Void> remove(List<Long> ids) {
        postService.remove(ids);
        return Result.ok();
    }
}
