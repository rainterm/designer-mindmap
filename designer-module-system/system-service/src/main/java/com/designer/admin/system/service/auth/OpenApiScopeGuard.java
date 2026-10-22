package com.designer.admin.system.service.auth;

import com.designer.admin.common.exception.BizException;
import com.designer.admin.framework.satoken.StpOAuthUtil;
import com.designer.admin.system.model.entity.auth.SysOauthScope;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.util.PathMatcher;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 开放接口：按路径匹配 Scope 目录，校验当前 access_token 是否具备对应 scope。
 *
 * @author 雨夜终端
 */
@Component
public class OpenApiScopeGuard {

    @Inject
    private SysOauthScopeService scopeService;

    public void checkCurrentRequest() {
        Context ctx = Context.current();
        if (ctx == null) {
            throw new BizException(403, "无法识别请求上下文");
        }
        String path = ctx.path();
        if (path == null) {
            throw new BizException(403, "无法识别请求路径");
        }
        List<SysOauthScope> scopes = scopeService.listAllEnabledPatterns();
        SysOauthScope matched = null;
        for (SysOauthScope scope : scopes) {
            String pattern = scope.getPathPattern();
            // 与 Sa-Token 的 Solon 插件同一套路由匹配器（支持 * / ** / {var}）
            if (pattern != null && PathMatcher.get(pattern).matches(path)) {
                // 取更具体（更长）的匹配
                if (matched == null || scope.getPathPattern().length() > matched.getPathPattern().length()) {
                    matched = scope;
                }
            }
        }
        if (matched == null) {
            throw new BizException(403, "接口未登记开放 Scope：" + path);
        }
        Set<String> held = new HashSet<>(StpOAuthUtil.getScopes());
        if (!held.contains(matched.getScopeCode())) {
            throw new BizException(403, "缺少 Scope 授权：" + matched.getScopeCode());
        }
    }
}
