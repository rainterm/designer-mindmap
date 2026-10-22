package com.designer.admin.system.config;

import cn.dev33.satoken.exception.NotLoginException;
import com.designer.admin.common.core.Result;
import com.designer.admin.common.core.ResultCode;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.common.util.JsonUtil;
import com.designer.admin.framework.satoken.StpOAuthUtil;
import com.designer.admin.system.service.auth.OpenApiScopeGuard;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.Filter;
import org.noear.solon.core.handle.FilterChain;

/**
 * /open-api/**：OAuth2 access_token 登录校验 + Scope 路径授权。
 *
 * <p>/open-api/oauth2/** 为令牌端点，不校验 access_token（换取 token 的前提）。</p>
 *
 * @author 雨夜终端
 */
@Component(index = -50)
public class OpenApiScopeFilter implements Filter {

    /** 开放接口路径前缀，只有这些请求才需要 OAuth2 Scope 校验 */
    private static final String OPEN_API_PREFIX = "/open-api/";

    /**
     * 令牌端点自身也挂在开放前缀下，但此时还没有 access_token；
     * 若一并拦截会形成「拿 token 必须先有 token」的死循环，必须放行。
     */
    private static final String OPEN_API_OAUTH2_PREFIX = "/open-api/oauth2/";

    @Inject
    private OpenApiScopeGuard scopeGuard;

    @Override
    public void doFilter(Context ctx, FilterChain chain) throws Throwable {
        String path = ctx.path();
        if (path != null && path.startsWith(OPEN_API_PREFIX) && !path.startsWith(OPEN_API_OAUTH2_PREFIX)) {
            try {
                // Postman 等会带 Bearer 前缀；写入 Storage 后再校验，避免读头失败
                StpOAuthUtil.checkLoginByAuthorization(ctx.header("Authorization"));
                scopeGuard.checkCurrentRequest();
            } catch (NotLoginException e) {
                write(ctx, Result.fail(ResultCode.UNAUTHORIZED, "未授权或 access_token 无效"));
                return;
            } catch (BizException e) {
                write(ctx, Result.fail(e.getCode(), e.getMessage()));
                return;
            }
        }
        chain.doFilter(ctx);
    }

    private void write(Context ctx, Result<?> body) {
        ctx.setRendered(false);
        ctx.contentType("application/json;charset=utf-8");
        ctx.output(JsonUtil.toJson(body));
        ctx.setRendered(true);
    }
}
