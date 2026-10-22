package com.designer.admin.system.controller.admin.rbac;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.Result;
import com.designer.admin.common.util.ExcelUtil;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.system.model.dto.rbac.ResetPwdDTO;
import com.designer.admin.system.model.dto.rbac.UserOptionQueryDTO;
import com.designer.admin.system.model.dto.rbac.UserQueryDTO;
import com.designer.admin.system.model.dto.rbac.UserSaveDTO;
import com.designer.admin.system.model.entity.rbac.SysUser;
import com.designer.admin.system.model.vo.rbac.UserExcelVO;
import com.designer.admin.system.model.vo.rbac.UserOptionVO;
import com.designer.admin.system.model.vo.common.ImportResultVO;
import com.designer.admin.system.service.rbac.SysUserService;
import org.noear.solon.annotation.Body;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Delete;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Post;
import org.noear.solon.annotation.Put;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.UploadedFile;
import org.noear.solon.validation.annotation.Valid;
import org.noear.solon.validation.annotation.Validated;

import java.util.List;

/**
 * 用户管理：CRUD + 重置密码 + 启停状态
 *
 * @author 雨夜终端
 */
@Valid
@Controller
@Mapping("/admin-api/system/user")
@Api(tags = "用户管理")
public class SysUserController {

    @Inject
    private SysUserService userService;

    @Get
    @Mapping("/page")
    @SaCheckPermission("system:user:list")
    @ApiOperation(value = "分页查询用户")
    public Result<PageResult<SysUser>> listPage(UserQueryDTO query) {
        return Result.ok(PageResult.of(userService.listPage(query)));
    }

    @Get
    @Mapping("/options")
    @ApiOperation(value = "分页查询用户下拉选项")
    public Result<PageResult<UserOptionVO>> listOptionPage(UserOptionQueryDTO query) {
        return Result.ok(PageResult.of(userService.listOptionPage(query)));
    }

    @Get
    @Mapping("/check-username")
    @SaCheckPermission("system:user:list")
    @ApiOperation(value = "校验用户名是否可用")
    public Result<Boolean> isUsernameAvailable(String username, Long excludeId) {
        return Result.ok(userService.isUsernameAvailable(username, excludeId));
    }

    @Get
    @Mapping("/{id}")
    @SaCheckPermission("system:user:query")
    @ApiOperation(value = "获取用户详情")
    public Result<SysUser> getDetail(Long id) {
        return Result.ok(userService.getById(id));
    }

    @Post
    @Mapping("/add")
    @OperLog(title = "用户管理", businessType = BusinessType.INSERT)
    @SaCheckPermission("system:user:add")
    @ApiOperation(value = "新增用户")
    public Result<Void> add(@Body @Validated UserSaveDTO dto) {
        userService.add(dto);
        return Result.ok();
    }

    @Put
    @Mapping("/update")
    @OperLog(title = "用户管理", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:user:edit")
    @ApiOperation(value = "修改用户")
    public Result<Void> update(@Body @Validated UserSaveDTO dto) {
        userService.update(dto);
        return Result.ok();
    }

    @Delete
    @Mapping("/remove")
    @OperLog(title = "用户管理", businessType = BusinessType.DELETE)
    @SaCheckPermission("system:user:remove")
    @ApiOperation(value = "删除用户")
    public Result<Void> remove(List<Long> ids) {
        userService.remove(ids);
        return Result.ok();
    }

    @Put
    @Mapping("/password")
    @OperLog(title = "用户管理", businessType = BusinessType.UPDATE, saveParam = false)
    @SaCheckPermission("system:user:resetPwd")
    @ApiOperation(value = "重置用户密码")
    public Result<Void> resetPassword(@Body @Validated ResetPwdDTO dto) {
        userService.resetPassword(dto);
        return Result.ok();
    }

    @Put
    @Mapping("/{id}/status")
    @SaCheckPermission("system:user:edit")
    @ApiOperation(value = "修改用户状态")
    public Result<Void> updateStatus(Long id, Integer status) {
        userService.updateStatus(id, status);
        return Result.ok();
    }

    @Get
    @Mapping("/export")
    @OperLog(title = "用户管理", businessType = BusinessType.EXPORT, saveResult = false)
    @SaCheckPermission("system:user:export")
    @ApiOperation(value = "导出用户数据")
    public void export(Context ctx, UserQueryDTO query) throws Exception {
        ExcelUtil.write(ctx, "用户数据.xlsx", "用户", UserExcelVO.class, userService.exportRows(query));
    }

    @Get
    @Mapping("/importTemplate")
    @SaCheckPermission("system:user:import")
    @ApiOperation(value = "下载用户导入模板")
    public void importTemplate(Context ctx) throws Exception {
        ExcelUtil.writeTemplate(ctx, "用户导入模板.xlsx", "用户", UserExcelVO.class);
    }

    @Post
    @Mapping("/import")
    @OperLog(title = "用户管理", businessType = BusinessType.IMPORT, saveParam = false, saveResult = false)
    @SaCheckPermission("system:user:import")
    @ApiOperation(value = "导入用户数据")
    public Result<ImportResultVO> importData(UploadedFile file) {
        int count = userService.importRows(ExcelUtil.read(file, UserExcelVO.class));
        return Result.ok(new ImportResultVO(count));
    }
}
