package com.designer.admin.system.controller.admin.monitor;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.Result;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.system.model.dto.monitor.OnlineUserQueryDTO;
import com.designer.admin.system.model.vo.monitor.OnlineUserVO;
import com.designer.admin.system.service.monitor.OnlineUserService;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Delete;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.validation.annotation.Valid;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 在线用户
 *
 * @author 雨夜终端
 */
@Valid
@Controller
@Mapping("/admin-api/system/online")
@Api(tags = "在线用户")
public class SysOnlineController {

    @Inject
    private OnlineUserService onlineUserService;

    @Get
    @Mapping("/page")
    @SaCheckPermission("system:online:list")
    @ApiOperation(value = "分页查询在线用户")
    public Result<PageResult<OnlineUserVO>> listPage(OnlineUserQueryDTO query) {
        return Result.ok(PageResult.of(onlineUserService.listPage(query)));
    }

    @Delete
    @Mapping("/{tokenId}")
    @OperLog(title = "在线用户", businessType = BusinessType.FORCE)
    @SaCheckPermission("system:online:forceLogout")
    @ApiOperation(value = "强制下线指定用户")
    public Result<Void> forceLogout(String tokenId) {
        onlineUserService.forceLogout(tokenId);
        return Result.ok();
    }

    @Delete
    @Mapping("/batch")
    @OperLog(title = "在线用户", businessType = BusinessType.FORCE)
    @SaCheckPermission("system:online:forceLogout")
    @ApiOperation(value = "批量强制下线")
    public Result<Void> forceLogoutBatch(List<String> tokenIds) {
        onlineUserService.forceLogoutBatch(tokenIds);
        return Result.ok();
    }

}
