package com.designer.admin.system.controller.admin.config;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaIgnore;
import com.designer.admin.common.core.Result;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.system.constant.SystemCacheConsts;
import com.designer.admin.system.model.dto.config.SystemSettingSaveDTO;
import com.designer.admin.system.model.entity.config.SysSetting;
import com.designer.admin.system.model.vo.config.SystemSettingVO;
import com.designer.admin.system.service.config.SysSettingService;
import org.noear.solon.annotation.Body;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Put;
import org.noear.solon.data.annotation.Cache;
import org.noear.solon.validation.annotation.Valid;
import org.noear.solon.validation.annotation.Validated;

/**
 * 系统设置（单行）：系统名称 / 登录页副标题 / Logo / 默认偏好设置
 *
 * @author 雨夜终端
 */
@Valid
@Controller
@Mapping("/admin-api/system/setting")
@Api(tags = "系统设置")
public class SysSettingController {

    @Inject
    private SysSettingService settingService;

    @SaIgnore
    @Get
    @Mapping("/public")
    @Cache(tags = SystemCacheConsts.SETTING_PUBLIC, seconds = SystemCacheConsts.CACHE_SECONDS)
    @ApiOperation(value = "获取公开系统设置")
    public Result<SystemSettingVO> getPublicSetting() {
        return Result.ok(settingService.getPublic());
    }

    @Get
    @Mapping("")
    @SaCheckPermission("system:setting:query")
    @ApiOperation(value = "获取系统设置详情")
    public Result<SysSetting> getDetail() {
        return Result.ok(settingService.getSetting());
    }

    @Put
    @Mapping("")
    @OperLog(title = "系统设置", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:setting:edit")
    @ApiOperation(value = "修改系统设置")
    public Result<Void> update(@Body @Validated SystemSettingSaveDTO dto) {
        settingService.update(dto);
        return Result.ok();
    }
}
