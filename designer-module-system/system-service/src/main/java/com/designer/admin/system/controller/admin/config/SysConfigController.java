package com.designer.admin.system.controller.admin.config;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.Result;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.system.model.dto.config.ConfigQueryDTO;
import com.designer.admin.system.model.dto.config.ConfigSaveDTO;
import com.designer.admin.system.model.entity.config.SysConfig;
import com.designer.admin.system.service.config.SysConfigService;
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
 * 参数配置
 *
 * @author 雨夜终端
 */
@Valid
@Controller
@Mapping("/admin-api/system/config")
@Api(tags = "参数配置")
public class SysConfigController {

    @Inject
    private SysConfigService configService;

    @Get
    @Mapping("/page")
    @SaCheckPermission("system:config:list")
    @ApiOperation(value = "分页查询参数配置")
    public Result<PageResult<SysConfig>> listPage(ConfigQueryDTO query) {
        return Result.ok(PageResult.of(configService.listPage(query)));
    }

    @Get
    @Mapping("/{id}")
    @SaCheckPermission("system:config:query")
    @ApiOperation(value = "获取参数配置详情")
    public Result<SysConfig> getDetail(Long id) {
        return Result.ok(configService.getById(id));
    }

    @Get
    @Mapping("/value/{configKey}")
    @ApiOperation(value = "根据键获取参数值")
    public Result<String> getValue(String configKey) {
        return Result.ok(configService.getValue(configKey));
    }

    @Post
    @Mapping("/add")
    @OperLog(title = "参数设置", businessType = BusinessType.INSERT)
    @SaCheckPermission("system:config:add")
    @ApiOperation(value = "新增参数配置")
    public Result<Void> add(@Body @Validated ConfigSaveDTO dto) {
        configService.add(dto);
        return Result.ok();
    }

    @Put
    @Mapping("/update")
    @OperLog(title = "参数设置", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:config:edit")
    @ApiOperation(value = "修改参数配置")
    public Result<Void> update(@Body @Validated ConfigSaveDTO dto) {
        configService.update(dto);
        return Result.ok();
    }

    @Delete
    @Mapping("/remove")
    @OperLog(title = "参数设置", businessType = BusinessType.DELETE)
    @SaCheckPermission("system:config:remove")
    @ApiOperation(value = "删除参数配置")
    public Result<Void> remove(List<Long> ids) {
        configService.remove(ids);
        return Result.ok();
    }

    @Delete
    @Mapping("/refresh")
    @OperLog(title = "参数设置", businessType = BusinessType.CLEAN)
    @SaCheckPermission("system:config:edit")
    @ApiOperation(value = "刷新参数缓存")
    public Result<Void> refreshCache() {
        configService.refreshCache();
        return Result.ok();
    }
}
