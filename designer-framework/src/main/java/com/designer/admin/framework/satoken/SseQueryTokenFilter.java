package com.designer.admin.framework.satoken;

import org.noear.solon.annotation.Component;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.Filter;
import org.noear.solon.core.handle.FilterChain;

import java.util.regex.Pattern;

/**
 * SSE 端点 query token → 请求头桥接（全局只放开这一个路径）。
 *
 * <p>{@code EventSource} 不能自定义请求头，所以浏览器只能把 token 放在 URL 上。但
 * Sa-Token 的 {@code is-read-body}（覆盖 query 参数与表单）是全局开关：一开，
 * 所有 {@code /admin-api/**} 都接受 {@code ?Authorization=}，token 会随 URL
 * 进入访问日志、Referer 与浏览器历史。</p>
 *
 * <p>因此全局保持 {@code is-read-body=false}，只在本过滤器里对
 * {@value #SSE_PATH} 把 query 参数提升为请求头，之后仍由 Sa-Token 做完整的登录校验与续期。
 * 其它路径把 token 放在 query 里不会被识别。</p>
 *
 * <p>order 必须早于 {@code SaTokenInterceptor(-100)}：拦截器读的是请求头，
 * 桥接晚一步就等于没做。</p>
 *
 * @author 雨夜终端
 */
@Component(index = -150)
public class SseQueryTokenFilter implements Filter {

    /** 通知 SSE 端点（与 SysNoticeController 的 @Mapping 保持一致） */
    private static final String SSE_PATH = "/admin-api/system/notice/sse";

    private static final String TOKEN_HEADER = "Authorization";

    /** token 由 Sa-Token 生成（uuid 风格），形状不对的直接忽略，避免拿垃圾值去查 Redis */
    private static final Pattern TOKEN_SHAPE = Pattern.compile("^[A-Za-z0-9_-]{8,128}$");

    @Override
    public void doFilter(Context ctx, FilterChain chain) throws Throwable {
        if (SSE_PATH.equals(ctx.path()) && isBlank(ctx.header(TOKEN_HEADER))) {
            String token = ctx.param(TOKEN_HEADER);
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
