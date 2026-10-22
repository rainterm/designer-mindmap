package com.designer.admin.framework.satoken;

import org.noear.solon.annotation.Component;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.Filter;
import org.noear.solon.core.handle.FilterChain;

import java.util.regex.Pattern;

/**
 * 私有下载路径的 Cookie → 请求头桥接。
 *
 * <p>只对 {@link DownloadTokenCookie#PATH} 生效：请求既没有 {@code Authorization} 头、
 * 也没有同名参数时，从 {@link DownloadTokenCookie#NAME} 取值写进请求头，
 * 之后仍由 Sa-Token 做完整的登录校验与活跃续期，不另开一套鉴权判断。</p>
 *
 * <p>order 必须早于 {@code SaTokenInterceptor(-100)}：拦截器读的是请求头，
 * 桥接晚一步就等于没做。</p>
 *
 * @author 雨夜终端
 */
@Component(index = -150)
public class DownloadTokenCookieFilter implements Filter {

    private static final String TOKEN_HEADER = "Authorization";

    /** token 由 Sa-Token 生成（uuid 风格），形状不对的直接忽略，避免拿垃圾值去查 Redis */
    private static final Pattern TOKEN_SHAPE = Pattern.compile("^[A-Za-z0-9_-]{8,128}$");

    @Override
    public void doFilter(Context ctx, FilterChain chain) throws Throwable {
        if (ctx.path().startsWith(DownloadTokenCookie.PATH)
                && isBlank(ctx.header(TOKEN_HEADER))
                && isBlank(ctx.param(TOKEN_HEADER))) {
            String token = ctx.cookie(DownloadTokenCookie.NAME);
            if (token != null && TOKEN_SHAPE.matcher(token).matches()) {
                // headerMap 是按请求缓存的 MultiMap，改它对后续拦截器与 controller 都可见
                ctx.headerMap().put(TOKEN_HEADER, token);
            }
        }
        chain.doFilter(ctx);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
