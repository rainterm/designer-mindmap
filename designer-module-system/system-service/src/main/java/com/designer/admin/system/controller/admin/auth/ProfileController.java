package com.designer.admin.system.controller.admin.auth;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import com.designer.admin.common.core.Result;
import com.designer.admin.framework.satoken.LoginHelper;
import com.designer.admin.system.model.dto.auth.PasswordUpdateDTO;
import com.designer.admin.system.model.dto.auth.ProfileUpdateDTO;
import com.designer.admin.system.model.vo.auth.UserInfoVO;
import com.designer.admin.system.service.auth.AuthService;
import com.designer.admin.system.service.rbac.SysUserService;
import org.noear.solon.annotation.Body;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Put;
import org.noear.solon.validation.annotation.Valid;
import org.noear.solon.validation.annotation.Validated;

/**
 * 个人中心：资料查看/修改、改密
 *
 * @author 雨夜终端
 */
@Valid
@Controller
@Mapping("/admin-api/system/profile")
@Api(tags = "个人中心")
public class ProfileController {

    @Inject
    private AuthService authService;

    @Inject
    private SysUserService userService;

    @Get
    @Mapping("")
    @ApiOperation(value = "获取个人资料")
    public Result<UserInfoVO> getProfile() {
        return Result.ok(authService.getUserInfo());
    }

    @Put
    @Mapping("/update")
    @ApiOperation(value = "修改个人资料")
    public Result<Void> update(@Body @Validated ProfileUpdateDTO dto) {
        userService.updateProfile(LoginHelper.getUserId(), dto);
        return Result.ok();
    }

    @Put
    @Mapping("/password")
    @ApiOperation(value = "修改登录密码")
    public Result<Void> updatePassword(@Body @Validated PasswordUpdateDTO dto) {
        userService.updatePassword(LoginHelper.getUserId(), dto.getOldPassword(), dto.getNewPassword());
        return Result.ok();
    }
}
