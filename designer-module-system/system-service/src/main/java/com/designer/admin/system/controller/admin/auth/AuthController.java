package com.designer.admin.system.controller.admin.auth;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import com.designer.admin.common.core.Result;
import com.designer.admin.framework.ratelimit.RateLimit;
import com.designer.admin.framework.ratelimit.RateLimitDimension;
import com.designer.admin.framework.satoken.DownloadTokenCookie;
import com.designer.admin.system.model.dto.auth.LoginDTO;
import com.designer.admin.system.model.dto.auth.SsoExchangeDTO;
import com.designer.admin.system.model.dto.auth.SsoTicketDTO;
import com.designer.admin.system.model.vo.auth.CaptchaVO;
import com.designer.admin.system.model.vo.auth.LoginVO;
import com.designer.admin.system.model.vo.auth.SsoTicketVO;
import com.designer.admin.system.model.vo.auth.UserInfoVO;
import com.designer.admin.system.model.vo.rbac.RouterVO;
import com.designer.admin.system.service.auth.AuthService;
import com.designer.admin.system.service.auth.SsoService;
import org.noear.solon.annotation.Body;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Post;
import org.noear.solon.core.handle.Context;
import org.noear.solon.validation.annotation.Length;
import org.noear.solon.validation.annotation.Valid;
import org.noear.solon.validation.annotation.Validated;

import java.util.List;

/**
 * 认证：登录、登出、当前用户、动态路由
 *
 * @author 雨夜终端
 */
@Valid
@Controller
@Mapping("/admin-api/system/auth")
@Api(tags = "认证授权")
public class AuthController {

    @Inject
    private AuthService authService;
    @Inject
    private SsoService ssoService;

    @SaIgnore
    @Get
    @Mapping("/captcha")
    @ApiOperation(value = "获取登录验证码")
    public Result<CaptchaVO> getCaptcha(@Length(max = 50, message = "账号长度不能超过 50 个字符") String username) {
        return Result.ok(authService.getCaptcha(username));
    }

    @SaIgnore
    @Post
    @Mapping("/login")
    @RateLimit(name = "system:login", dimension = RateLimitDimension.IP, rules = {
            @RateLimit.Rule(count = 10, window = 60, message = "登录尝试过于频繁，请 {} 秒后再试"),
            @RateLimit.Rule(count = 100, window = 24 * 60 * 60, message = "24 小时内登录尝试次数过多，请稍后再试")
    })
    @ApiOperation(value = "账号密码登录")
    public Result<LoginVO> login(@Body @Validated LoginDTO dto, Context ctx) {
        LoginVO vo = authService.login(dto, ctx);
        DownloadTokenCookie.write(ctx, vo.getToken(), vo.getExpiresIn());
        return Result.ok(vo);
    }

    @Post
    @Mapping("/logout")
    @ApiOperation(value = "退出登录")
    public Result<Void> logout(Context ctx) {
        StpUtil.logout();
        DownloadTokenCookie.clear(ctx);
        return Result.ok();
    }

    @Get
    @Mapping("/info")
    @ApiOperation(value = "获取当前登录用户信息")
    public Result<UserInfoVO> getInfo() {
        return Result.ok(authService.getUserInfo());
    }

    @Get
    @Mapping("/routers")
    @ApiOperation(value = "获取当前用户动态路由")
    public Result<List<RouterVO>> listRouters() {
        return Result.ok(authService.listRouters());
    }

    @Post
    @Mapping("/sso/ticket")
    @ApiOperation(value = "签发一次性登录票据")
    public Result<SsoTicketVO> ssoTicket(@Body @Validated SsoTicketDTO dto) {
        return Result.ok(ssoService.issueTicket(dto == null ? null : dto.getRedirect()));
    }

    @SaIgnore
    @Post
    @Mapping("/sso/exchange")
    @ApiOperation(value = "一次性票据换取令牌")
    public Result<LoginVO> ssoExchange(@Body @Validated SsoExchangeDTO dto, Context ctx) {
        LoginVO vo = ssoService.exchange(dto == null ? null : dto.getTicket());
        DownloadTokenCookie.write(ctx, vo.getToken(), vo.getExpiresIn());
        return Result.ok(vo);
    }
}
