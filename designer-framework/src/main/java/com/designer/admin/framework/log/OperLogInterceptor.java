package com.designer.admin.framework.log;

import com.designer.admin.common.util.JsonUtil;
import com.designer.admin.framework.satoken.LoginHelper;
import org.noear.solon.Solon;
import org.noear.solon.core.aspect.Invocation;
import org.noear.solon.core.aspect.MethodInterceptor;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.UploadedFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 操作日志环绕拦截器：记录方法、URL、IP、操作人、参数、结果、耗时、异常。
 *
 * @author 雨夜终端
 */
public class OperLogInterceptor implements MethodInterceptor {

    private static final int MAX_TEXT = 2000;

    @Override
    public Object doIntercept(Invocation inv) throws Throwable {
        OperLog anno = inv.getMethodAnnotation(OperLog.class);
        Context ctx = Context.current();

        long start = System.currentTimeMillis();
        Object result = null;
        Throwable error = null;
        try {
            result = inv.invoke();
            return result;
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            try {
                record(anno, ctx, inv, result, error, System.currentTimeMillis() - start);
            } catch (Exception ignored) {
                // 日志记录失败不影响主流程
            }
        }
    }

    private void record(OperLog anno, Context ctx, Invocation inv, Object result, Throwable error, long cost) {
        if (anno == null) {
            return;
        }
        OperLogRecord log = new OperLogRecord();
        log.setTitle(anno.title());
        log.setBusinessType(anno.businessType());
        log.setRequestMethod(ctx == null ? "" : ctx.method());
        log.setOperUrl(ctx == null ? "" : ctx.path());
        log.setOperIp(ctx == null ? "" : ctx.realIp());
        log.setMethod(inv.method().getDeclaringClz().getName() + "." + inv.method().getMethod().getName());
        log.setStatus(error == null ? 1 : 0);
        log.setCostTime(cost);
        log.setOperTime(LocalDateTime.now());
        try {
            log.setOperName(LoginHelper.getUsername());
        } catch (Exception e) {
            log.setOperName("");
        }
        if (anno.saveParam()) {
            log.setOperParam(truncate(JsonUtil.toJson(filterArgs(inv.args()))));
        }
        if (anno.saveResult() && result != null) {
            log.setJsonResult(truncate(JsonUtil.toJson(result)));
        }
        if (error != null) {
            log.setErrorMsg(truncate(error.getMessage()));
        }

        OperLogWriter writer = Solon.context().getBean(OperLogWriter.class);
        if (writer != null) {
            writer.writeAsync(log);
        }
    }

    private List<Object> filterArgs(Object[] args) {
        List<Object> list = new ArrayList<>();
        if (args != null) {
            for (Object arg : args) {
                if (arg instanceof Context || arg instanceof UploadedFile || arg instanceof String[][]) {
                    continue;
                }
                list.add(arg);
            }
        }
        return list;
    }

    private String truncate(String text) {
        if (text == null) {
            return null;
        }
        return text.length() > MAX_TEXT ? text.substring(0, MAX_TEXT) : text;
    }
}
