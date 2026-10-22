package com.designer.admin.framework.satoken;

import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.Cookie;

/**
 * 私有文件下载专用的 token Cookie。
 *
 * <p>浏览器直接打开 {@code /admin-api/system/file/download/**}（新标签页、{@code <img>} 回显）
 * 时带不了 {@code Authorization} 请求头，这里把登录 token 额外写进一份 Cookie，
 * 由 {@link DownloadTokenCookieFilter} 在该路径上提升为请求头。</p>
 *
 * <p>为什么不直接把 Sa-Token 配成 {@code is-read-cookie: true}：那会让整个
 * {@code /admin-api/**} 都接受 Cookie 鉴权，等于把所有后台接口暴露在 CSRF 之下。
 * 这里用独立 Cookie 名 + {@code Path} 限定，只对下载路径生效，业务接口仍只认请求头。</p>
 *
 * <p>Cookie 为 HttpOnly：前端拿不到它，token 仍以 localStorage 为准，两者互不干扰。
 * 未显式声明 SameSite，主流浏览器按 Lax 处理 —— 跨站图片/表单不会带上它，
 * 只有本站请求和顶层跳转会带上。</p>
 *
 * @author 雨夜终端
 */
public final class DownloadTokenCookie {

    /** Cookie 名：特意与 tokenName（Authorization）不同，避免被其它接口顺带认成凭据 */
    public static final String NAME = "designer_download_token";

    /** 只在私有下载接口上发送 */
    public static final String PATH = "/admin-api/system/file/download/";

    private DownloadTokenCookie() {
    }

    /** 登录成功后写入；maxAge 与 token 有效期一致，token 过期后 Cookie 自然作废 */
    public static void write(Context ctx, String token, long maxAgeSeconds) {
        if (ctx == null || token == null || token.isBlank()) {
            return;
        }
        int maxAge = (int) Math.min(Math.max(maxAgeSeconds, 1L), Integer.MAX_VALUE);
        ctx.cookieSet(base(ctx, token).maxAge(maxAge));
    }

    /** 注销时删除；HttpOnly 的 Cookie 只能由服务端清除 */
    public static void clear(Context ctx) {
        if (ctx == null) {
            return;
        }
        ctx.cookieSet(base(ctx, "").maxAge(0));
    }

    private static Cookie base(Context ctx, String value) {
        return new Cookie(NAME, value)
                .path(PATH)
                .httpOnly(true)
                .secure(isSecure(ctx));
    }

    /**
     * 是否加 Secure：本地 http 调试不能加（否则浏览器直接丢弃），生产走 https 必须加。
     *
     * <p>线上通常是 nginx 终止 TLS、反代给 http 端口的后端，此时 {@code ctx.isSecure()} 为 false，
     * 要再看 {@code X-Forwarded-Proto}（deploy/nginx 已用 {@code $scheme} 覆盖写入，客户端伪造不生效）。</p>
     */
    private static boolean isSecure(Context ctx) {
        return ctx.isSecure() || "https".equalsIgnoreCase(ctx.header("X-Forwarded-Proto"));
    }
}
