package com.designer.admin.framework.exception;

import com.designer.admin.common.core.Result;
import com.designer.admin.common.core.ResultCode;
import com.designer.admin.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.Filter;
import org.noear.solon.core.handle.FilterChain;
import org.noear.solon.validation.ValidatorException;

/**
 * 全局异常过滤器：把 Controller 抛出的异常统一转成 Result 结构（HTTP 200 + 业务码）。
 *
 * @author 雨夜终端
 */
@Component(index = -200)
@Slf4j
public class GlobalExceptionFilter implements Filter {

    @Override
    public void doFilter(Context ctx, FilterChain chain) throws Throwable {
        try {
            chain.doFilter(ctx);
        } catch (BizException e) {
            render(ctx, Result.fail(e.getCode(), e.getMessage()));
        } catch (ValidatorException e) {
            render(ctx, Result.fail(ResultCode.BAD_REQUEST, "参数校验失败：" + e.getMessage()));
        } catch (Throwable e) {
            // 登录/权限异常已由 Sa-Token 拦截器处理，这里兜底其余未知异常。
            // 详情只进日志：把 e.getMessage() 回传会把 SQL 片段、类名、路径等内部信息泄露给匿名调用方。
            log.error("未处理异常 {} {}", ctx.method(), ctx.path(), e);
            render(ctx, Result.fail(ResultCode.ERROR, "系统繁忙，请稍后重试"));
        }
    }

    /**
     * 交给 Solon 的渲染链（snack4）输出：contentType 与「已渲染」标记由框架维护，
     * 避免手写 JSON 时漏掉其中一步导致响应被写两次。
     */
    private void render(Context ctx, Result<?> body) {
        ctx.setRendered(false);
        try {
            ctx.render(body);
        } catch (Throwable e) {
            // 渲染本身失败时退回最简输出，至少让调用方拿到业务码而不是空响应
            log.warn("异常响应渲染失败 {} {}", ctx.method(), ctx.path(), e);
            ctx.setRendered(false);
            ctx.contentType("application/json;charset=utf-8");
            ctx.output(com.designer.admin.common.util.JsonUtil.toJson(body));
            ctx.setRendered(true);
        }
    }
}
