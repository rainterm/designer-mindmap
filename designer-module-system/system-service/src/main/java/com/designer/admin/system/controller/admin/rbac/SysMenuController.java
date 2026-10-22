package com.designer.admin.system.controller.admin.rbac;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.Result;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.system.model.dto.rbac.MenuSaveDTO;
import com.designer.admin.system.model.entity.rbac.SysMenu;
import com.designer.admin.system.service.rbac.SysMenuService;
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
 * 菜单管理：CRUD + 菜单树（前端权限与路由源数据）
 *
 * @author 雨夜终端
 */
@Valid
@Controller
@Mapping("/admin-api/system/menu")
@Api(tags = "菜单管理")
public class SysMenuController {

    @Inject
    private SysMenuService menuService;

    @Get
    @Mapping("/tree")
    @SaCheckPermission("system:menu:list")
    @ApiOperation(value = "查询菜单树")
    public Result<List<SysMenu>> listTree(String keyword, Integer status) {
        return Result.ok(menuService.listTree(keyword, status));
    }

    @Get
    @Mapping("/options")
    @ApiOperation(value = "查询菜单下拉选项")
    public Result<List<SysMenu>> listOptions() {
        return Result.ok(menuService.listOptions());
    }

    @Get
    @Mapping("/{id}")
    @SaCheckPermission("system:menu:query")
    @ApiOperation(value = "获取菜单详情")
    public Result<SysMenu> getDetail(Long id) {
        return Result.ok(menuService.getById(id));
    }

    @Post
    @Mapping("/add")
    @OperLog(title = "菜单管理", businessType = BusinessType.INSERT)
    @SaCheckPermission("system:menu:add")
    @ApiOperation(value = "新增菜单")
    public Result<Void> add(@Body @Validated MenuSaveDTO dto) {
        menuService.add(dto);
        return Result.ok();
    }

    @Put
    @Mapping("/update")
    @OperLog(title = "菜单管理", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:menu:edit")
    @ApiOperation(value = "修改菜单")
    public Result<Void> update(@Body @Validated MenuSaveDTO dto) {
        menuService.update(dto);
        return Result.ok();
    }

    @Delete
    @Mapping("/{id}")
    @OperLog(title = "菜单管理", businessType = BusinessType.DELETE)
    @SaCheckPermission("system:menu:remove")
    @ApiOperation(value = "删除菜单")
    public Result<Void> remove(Long id) {
        menuService.remove(id);
        return Result.ok();
    }
}
