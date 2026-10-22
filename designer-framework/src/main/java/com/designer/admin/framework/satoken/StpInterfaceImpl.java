package com.designer.admin.framework.satoken;

import cn.dev33.satoken.stp.StpInterface;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import java.util.Collections;
import java.util.List;

/**
 * Sa-Token 权限数据源：为 @SaCheckPermission / @SaCheckRole 提供权限与角色清单。
 * 超级管理员直接返回通配符 * ，跳过细粒度校验。
 *
 * <p>这里只做「登录 ID → 用户 ID」的转换与空值兜底；
 * 缓存与回源都在 {@link PermissionLoader} 的实现里，本类保持无状态。</p>
 *
 * @author 雨夜终端
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    @Inject
    private PermissionLoader permissionLoader;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        PermissionSnapshot snapshot = load(loginId);
        return snapshot == null ? Collections.emptyList() : snapshot.getPermissions();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        PermissionSnapshot snapshot = load(loginId);
        return snapshot == null ? Collections.emptyList() : snapshot.getRoles();
    }

    private PermissionSnapshot load(Object loginId) {
        Long userId = toUserId(loginId);
        if (userId == null) {
            return null;
        }
        return permissionLoader.load(userId);
    }

    private Long toUserId(Object loginId) {
        if (loginId == null) {
            return null;
        }
        try {
            return Long.valueOf(loginId.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
