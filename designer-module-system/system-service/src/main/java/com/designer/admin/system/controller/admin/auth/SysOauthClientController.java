package com.designer.admin.system.controller.admin.auth;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.Result;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.system.model.dto.auth.OauthClientQueryDTO;
import com.designer.admin.system.model.dto.auth.OauthClientSaveDTO;
import com.designer.admin.system.model.entity.auth.SysOauthClient;
import com.designer.admin.system.model.vo.auth.OauthClientCreatedVO;
import com.designer.admin.system.service.auth.SysOauthClientService;
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
 * OAuth2 客户端后台接口。
 *
 * @author 雨夜终端
 */
@Valid
@Controller
@Mapping("/admin-api/system/oauth-client")
@Api(tags = "OAuth2 客户端")
public class SysOauthClientController {

    @Inject
    private SysOauthClientService clientService;

    @Get
    @Mapping("/page")
    @SaCheckPermission("system:oauthclient:list")
    @ApiOperation(value = "分页查询 OAuth2 客户端")
    public Result<PageResult<SysOauthClient>> page(OauthClientQueryDTO query) {
        return Result.ok(PageResult.of(clientService.listPage(query)));
    }

    @Get
    @Mapping("/{id}")
    @SaCheckPermission("system:oauthclient:query")
    @ApiOperation(value = "获取 OAuth2 客户端详情")
    public Result<SysOauthClient> detail(Long id) {
        return Result.ok(clientService.getById(id));
    }

    @Post
    @Mapping("/add")
    @OperLog(title = "OAuth2 客户端", businessType = BusinessType.INSERT)
    @SaCheckPermission("system:oauthclient:add")
    @ApiOperation(value = "新增 OAuth2 客户端")
    public Result<OauthClientCreatedVO> add(@Body @Validated OauthClientSaveDTO dto) {
        return Result.ok(clientService.add(dto));
    }

    @Put
    @Mapping("/update")
    @OperLog(title = "OAuth2 客户端", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:oauthclient:edit")
    @ApiOperation(value = "修改 OAuth2 客户端")
    public Result<Void> update(@Body @Validated OauthClientSaveDTO dto) {
        clientService.update(dto);
        return Result.ok();
    }

    @Post
    @Mapping("/{id}/reset-secret")
    @OperLog(title = "OAuth2 客户端", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:oauthclient:resetSecret")
    @ApiOperation(value = "重置 OAuth2 客户端密钥")
    public Result<OauthClientCreatedVO> resetSecret(Long id) {
        return Result.ok(clientService.resetSecret(id));
    }

    @Delete
    @Mapping("/remove")
    @OperLog(title = "OAuth2 客户端", businessType = BusinessType.DELETE)
    @SaCheckPermission("system:oauthclient:remove")
    @ApiOperation(value = "删除 OAuth2 客户端")
    public Result<Void> remove(List<Long> ids) {
        clientService.remove(ids);
        return Result.ok();
    }
}
