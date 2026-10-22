package com.designer.admin.framework.ratelimit;

import com.designer.admin.common.core.ResultCode;
import com.designer.admin.common.exception.BizException;

/**
 * 触发限流。
 *
 * <p>继承 {@code BizException} 是为了复用全局异常处理（{@code GlobalExceptionFilter}）：
 * 无需为限流单独加一个 Filter 分支，异常会自动变成
 * {@code {"code":429,"msg":"..."}} 的响应体。</p>
 *
 * <p>业务码取 429，与 nginx 层的 {@code limit_req_status 429} 对齐
 * （见 {@code deploy/nginx/default.conf}），这样「被限流」和「站点真的挂了」
 * 在两层之间有同一个含义。</p>
 *
 * <p>注意两层的**载体不同**：nginx 直接回 HTTP 429，而应用层沿用本项目的约定
 * ——{@code GlobalExceptionFilter} 把异常统一渲染成 HTTP 200 + 业务码，
 * 所以这里只体现在响应体的 {@code code} 上。按 HTTP 状态码判断的调用方
 * 只能看到 nginx 那一层，按业务码判断的（本项目前端）两层都能覆盖。</p>
 *
 * @author 雨夜终端
 */
public class RateLimitException extends BizException {

    public RateLimitException(String message) {
        super(ResultCode.TOO_MANY_REQUESTS, message);
    }
}
