package com.designer.admin.framework.satoken;

import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.solon.integration.SaTokenInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import com.designer.admin.common.core.Result;
import com.designer.admin.common.core.ResultCode;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Configuration;

/**
 * Sa-Token 拦截鉴权配置：
 * <ul>
 *   <li>{@code /admin-api/**} 默认需后台用户登录</li>
 *   <li>{@code /app-api/**} 面向前台，默认免登录（由业务控制）</li>
 *   <li>{@code /open-api/**} 由 {@code OpenApiScopeFilter} 做 OAuth2 登录 + Scope 校验
 *       （其中 {@code /open-api/oauth2/**} 为令牌端点，免校验）</li>
 * </ul>
 *
 * @author 雨夜终端
 */
@Configuration
public class SaTokenConfig {

    @Bean(index = -100)
    public SaTokenInterceptor saTokenInterceptor() {
        return new SaTokenInterceptor()
                .addInclude("/admin-api/**")
                .addInclude("/app-api/**")
                .addExclude("/open-api/**")
                .setAuth(req -> SaRouter.match("/admin-api/**", r -> StpUtil.checkLogin()))
                .setError(e -> {
                    int code = (e instanceof cn.dev33.satoken.exception.NotLoginException)
                            ? ResultCode.UNAUTHORIZED : ResultCode.FORBIDDEN;
                    return Result.fail(code, code == ResultCode.UNAUTHORIZED ? "登录状态已过期，请重新登录" : "无权限访问");
                });
    }
}
