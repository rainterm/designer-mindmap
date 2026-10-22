package com.designer.admin.system.controller.admin.rbac;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.Result;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.system.model.dto.rbac.RoleQueryDTO;
import com.designer.admin.system.model.dto.rbac.RoleSaveDTO;
import com.designer.admin.system.model.entity.rbac.SysRole;
import com.designer.admin.system.service.rbac.SysRoleService;
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
 * 角色管理：CRUD + 启停 + 角色授权菜单
 *
 * @author 雨夜终端
 */
@Valid
@Controller
@Mapping("/admin-api/system/role")
@Api(tags = "角色管理")
public class SysRoleController {

    @Inject
    private SysRoleService roleService;

    @Get
    @Mapping("/page")
    @SaCheckPermission("system:role:list")
    @ApiOperation(value = "分页查询角色")
    public Result<PageResult<SysRole>> listPage(RoleQueryDTO query) {
        return Result.ok(PageResult.of(roleService.listPage(query)));
    }

    @Get
    @Mapping("/options")
    @ApiOperation(value = "查询角色下拉选项")
    public Result<List<SysRole>> listOptions() {
        return Result.ok(roleService.listOptions());
    }

    @Get
    @Mapping("/{id}")
    @SaCheckPermission("system:role:query")
    @ApiOperation(value = "获取角色详情")
    public Result<SysRole> getDetail(Long id) {
        return Result.ok(roleService.getById(id));
    }

    @Post
    @Mapping("/add")
    @OperLog(title = "角色管理", businessType = BusinessType.INSERT)
    @SaCheckPermission("system:role:add")
    @ApiOperation(value = "新增角色")
    public Result<Void> add(@Body @Validated RoleSaveDTO dto) {
        roleService.add(dto);
        return Result.ok();
    }

    @Put
    @Mapping("/update")
    @OperLog(title = "角色管理", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:role:edit")
    @ApiOperation(value = "修改角色")
    public Result<Void> update(@Body @Validated RoleSaveDTO dto) {
        roleService.update(dto);
        return Result.ok();
    }

    @Delete
    @Mapping("/remove")
    @OperLog(title = "角色管理", businessType = BusinessType.DELETE)
    @SaCheckPermission("system:role:remove")
    @ApiOperation(value = "删除角色")
    public Result<Void> remove(List<Long> ids) {
        roleService.remove(ids);
        return Result.ok();
    }

    @Get
    @Mapping("/{id}/menus")
    @SaCheckPermission("system:role:query")
    @ApiOperation(value = "查询角色已授权菜单")
    public Result<List<Long>> listMenuIds(Long id) {
        return Result.ok(roleService.listMenuIds(id));
    }

    @Get
    @Mapping("/{id}/depts")
    @SaCheckPermission("system:role:query")
    @ApiOperation(value = "查询角色自定义数据权限的部门")
    public Result<List<Long>> listDeptIds(Long id) {
        return Result.ok(roleService.listDeptIds(id));
    }

    @Put
    @Mapping("/{id}/menus")
    @OperLog(title = "角色授权", businessType = BusinessType.GRANT)
    @SaCheckPermission("system:role:edit")
    @ApiOperation(value = "分配角色菜单权限")
    public Result<Void> assignMenus(Long id, @Body List<Long> menuIds) {
        roleService.assignMenus(id, menuIds);
        return Result.ok();
    }
}
