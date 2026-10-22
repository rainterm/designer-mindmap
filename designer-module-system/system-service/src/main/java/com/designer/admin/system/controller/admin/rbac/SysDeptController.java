package com.designer.admin.system.controller.admin.rbac;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.Result;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.system.model.dto.rbac.DeptSaveDTO;
import com.designer.admin.system.model.entity.rbac.SysDept;
import com.designer.admin.system.service.rbac.SysDeptService;
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
 * 部门管理
 *
 * @author 雨夜终端
 */
@Valid
@Controller
@Mapping("/admin-api/system/dept")
@Api(tags = "部门管理")
public class SysDeptController {

    @Inject
    private SysDeptService deptService;

    @Get
    @Mapping("/tree")
    @SaCheckPermission("system:dept:list")
    @ApiOperation(value = "查询部门树")
    public Result<List<SysDept>> listTree(String deptName, Integer status) {
        return Result.ok(deptService.listTree(deptName, status));
    }

    @Get
    @Mapping("/options")
    @ApiOperation(value = "查询部门下拉选项")
    public Result<List<SysDept>> listOptions() {
        return Result.ok(deptService.listOptions());
    }

    @Get
    @Mapping("/{id}")
    @SaCheckPermission("system:dept:query")
    @ApiOperation(value = "获取部门详情")
    public Result<SysDept> getDetail(Long id) {
        return Result.ok(deptService.getById(id));
    }

    @Post
    @Mapping("/add")
    @OperLog(title = "部门管理", businessType = BusinessType.INSERT)
    @SaCheckPermission("system:dept:add")
    @ApiOperation(value = "新增部门")
    public Result<Void> add(@Body @Validated DeptSaveDTO dto) {
        deptService.add(dto);
        return Result.ok();
    }

    @Put
    @Mapping("/update")
    @OperLog(title = "部门管理", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:dept:edit")
    @ApiOperation(value = "修改部门")
    public Result<Void> update(@Body @Validated DeptSaveDTO dto) {
        deptService.update(dto);
        return Result.ok();
    }

    @Delete
    @Mapping("/{id}")
    @OperLog(title = "部门管理", businessType = BusinessType.DELETE)
    @SaCheckPermission("system:dept:remove")
    @ApiOperation(value = "删除部门")
    public Result<Void> remove(Long id) {
        deptService.remove(id);
        return Result.ok();
    }
}
