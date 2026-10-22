package com.designer.admin.framework.satoken;

/**
 * 权限数据源 SPI：由 system 模块实现，framework 不直接依赖 Sys* Mapper。
 *
 * <p>一次返回 {@link PermissionSnapshot} 而不是三个独立方法，是为了让缓存与装载都能原子完成：
 * 「是否超管」和「权限清单」若分开取，缓存失效的瞬间可能读到一半新一半旧的数据。</p>
 *
 * @author 雨夜终端
 */
public interface PermissionLoader {

    /**
     * 加载用户权限快照。
     *
     * <p>实现应自带缓存；无角色用户也必须返回非 {@code null} 的空快照，
     * 否则每次鉴权都会回源数据库。</p>
     *
     * @param userId 用户 ID
     * @return 权限快照，不能为 {@code null}
     */
    PermissionSnapshot load(Long userId);
}
