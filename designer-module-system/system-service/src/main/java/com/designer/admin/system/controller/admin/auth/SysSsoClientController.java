package com.designer.admin.system.controller.admin.auth;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.Result;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.system.model.dto.auth.SsoClientQueryDTO;
import com.designer.admin.system.model.dto.auth.SsoClientSaveDTO;
import com.designer.admin.system.model.entity.auth.SysSsoClient;
import com.designer.admin.system.service.auth.SysSsoClientService;
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
 * SSO 客户端管理：白名单 CRUD
 *
 * @author 雨夜终端
 */
@Valid
@Controller
@Mapping("/admin-api/system/sso-client")
@Api(tags = "SSO 客户端")
public class SysSsoClientController {

    @Inject
    private SysSsoClientService ssoClientService;

    @Get
    @Mapping("/page")
    @SaCheckPermission("system:ssoclient:list")
    @ApiOperation(value = "分页查询 SSO 客户端")
    public Result<PageResult<SysSsoClient>> listPage(SsoClientQueryDTO query) {
        return Result.ok(PageResult.of(ssoClientService.listPage(query)));
    }

    @Get
    @Mapping("/enabled")
    @ApiOperation(value = "查询启用的 SSO 客户端")
    public Result<List<SysSsoClient>> listEnabled() {
        return Result.ok(ssoClientService.listEnabled());
    }

    @Get
    @Mapping("/by-key/{clientKey}")
    @ApiOperation(value = "根据标识获取 SSO 客户端")
    public Result<SysSsoClient> getByKey(String clientKey) {
        return Result.ok(ssoClientService.getEnabledByKey(clientKey));
    }

    @Get
    @Mapping("/{id}")
    @SaCheckPermission("system:ssoclient:query")
    @ApiOperation(value = "获取 SSO 客户端详情")
    public Result<SysSsoClient> getDetail(Long id) {
        return Result.ok(ssoClientService.getById(id));
    }

    @Post
    @Mapping("/add")
    @OperLog(title = "SSO客户端", businessType = BusinessType.INSERT)
    @SaCheckPermission("system:ssoclient:add")
    @ApiOperation(value = "新增 SSO 客户端")
    public Result<Void> add(@Body @Validated SsoClientSaveDTO dto) {
        ssoClientService.add(dto);
        return Result.ok();
    }

    @Put
    @Mapping("/update")
    @OperLog(title = "SSO客户端", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:ssoclient:edit")
    @ApiOperation(value = "修改 SSO 客户端")
    public Result<Void> update(@Body @Validated SsoClientSaveDTO dto) {
        ssoClientService.update(dto);
        return Result.ok();
    }

    @Delete
    @Mapping("/remove")
    @OperLog(title = "SSO客户端", businessType = BusinessType.DELETE)
    @SaCheckPermission("system:ssoclient:remove")
    @ApiOperation(value = "删除 SSO 客户端")
    public Result<Void> remove(List<Long> ids) {
        ssoClientService.remove(ids);
        return Result.ok();
    }
}
