package com.designer.admin.system.controller.admin.auth;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.Result;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.system.model.dto.auth.OauthScopeQueryDTO;
import com.designer.admin.system.model.dto.auth.OauthScopeSaveDTO;
import com.designer.admin.system.model.entity.auth.SysOauthScope;
import com.designer.admin.system.service.auth.SysOauthScopeService;
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
 * 开放接口 Scope：挂在 OAuth2 客户端弹框内管理，无独立菜单。
 *
 * @author 雨夜终端
 */
@Valid
@Controller
@Mapping("/admin-api/system/oauth-scope")
@Api(tags = "OAuth2 授权范围")
public class SysOauthScopeController {

    @Inject
    private SysOauthScopeService scopeService;

    @Get
    @Mapping("/page")
    @SaCheckPermission("system:oauthclient:list")
    @ApiOperation(value = "分页查询授权范围")
    public Result<PageResult<SysOauthScope>> listPage(OauthScopeQueryDTO query) {
        return Result.ok(PageResult.of(scopeService.listPage(query)));
    }

    @Get
    @Mapping("/options")
    @SaCheckPermission(value = {"system:oauthclient:list", "system:oauthclient:add", "system:oauthclient:edit"}, mode = SaMode.OR)
    @ApiOperation(value = "查询授权范围选项")
    public Result<List<SysOauthScope>> listOptions() {
        return Result.ok(scopeService.listEnabled());
    }

    @Get
    @Mapping("/{id}")
    @SaCheckPermission("system:oauthclient:query")
    @ApiOperation(value = "获取授权范围详情")
    public Result<SysOauthScope> getDetail(Long id) {
        return Result.ok(scopeService.getById(id));
    }

    @Post
    @Mapping("/add")
    @OperLog(title = "OAuth2 开放接口", businessType = BusinessType.INSERT)
    @SaCheckPermission(value = {"system:oauthclient:add", "system:oauthclient:edit"}, mode = SaMode.OR)
    @ApiOperation(value = "新增授权范围")
    public Result<SysOauthScope> add(@Body @Validated OauthScopeSaveDTO dto) {
        return Result.ok(scopeService.add(dto));
    }

    @Put
    @Mapping("/update")
    @OperLog(title = "OAuth2 开放接口", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:oauthclient:edit")
    @ApiOperation(value = "修改授权范围")
    public Result<Void> update(@Body @Validated OauthScopeSaveDTO dto) {
        scopeService.update(dto);
        return Result.ok();
    }

    @Delete
    @Mapping("/remove")
    @OperLog(title = "OAuth2 开放接口", businessType = BusinessType.DELETE)
    @SaCheckPermission("system:oauthclient:edit")
    @ApiOperation(value = "删除授权范围")
    public Result<Void> remove(List<Long> ids) {
        scopeService.remove(ids);
        return Result.ok();
    }
}
