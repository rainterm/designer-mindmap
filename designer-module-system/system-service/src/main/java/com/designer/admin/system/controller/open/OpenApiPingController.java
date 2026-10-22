package com.designer.admin.system.controller.open;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import com.designer.admin.common.core.Result;
import com.designer.admin.framework.satoken.StpOAuthUtil;
import com.designer.admin.system.model.vo.open.OpenApiPingVO;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Mapping;

/**
 * 开放接口示例：需 OAuth2 token + Scope open.ping
 *
 * @author 雨夜终端
 */
@Controller
@Mapping("/open-api/system")
@Api(tags = "开放接口")
public class OpenApiPingController {

    @Get
    @Mapping("/ping")
    @ApiOperation(value = "开放接口连通性测试")
    public Result<OpenApiPingVO> ping() {
        return Result.ok(OpenApiPingVO.builder()
                .ok(true)
                .clientId(StpOAuthUtil.getClientId())
                .scopes(StpOAuthUtil.getScopes())
                .serverTime(System.currentTimeMillis())
                .build());
    }
}
