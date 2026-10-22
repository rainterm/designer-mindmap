package com.designer.admin.framework.satoken;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 用户权限快照：一次鉴权所需的全部事实（是否超管、权限标识、角色标识、数据权限范围）。
 *
 * <p><b>为什么是四合一的快照</b>：{@code @SaCheckPermission} 的校验链路是先问「是否超管」、
 * 再取权限清单，分两次查库就是两次往返；数据权限还要再算一遍角色的 data_scope 与部门子树。
 * 把四者收进一个快照，缓存未命中时仍然只做一次装载（命中则只读一个 Redis 键），
 * 也避免「超管标记来自新数据、权限清单来自旧数据」这种半新半旧的组合。</p>
 *
 * <p><b>部门子树在装载时展开</b>：{@code DEPT_AND_CHILD} 的可见部门集合在回源时一次性算好，
 * 查询侧只拿一个 ID 集合去 {@code IN} 过滤。否则每个列表请求都要再查一次部门树，
 * 数据权限会把鉴权从 1 次 Redis GET 打回 2~3 次 SQL。</p>
 *
 * <p>字段名即缓存 JSON 的契约：改名等于让滚动发布期间的新旧实例互相读不懂缓存，
 * 只能等 TTL 过期。新增字段时旧 JSON 会反序列化为 {@code null}，
 * 所以 {@link #dataScopeUnrestricted()} 等访问器对 {@code null} 有明确的兜底语义。</p>
 *
 * @author 雨夜终端
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PermissionSnapshot implements Serializable {

    /** 是否超级管理员（拥有全部权限，且不受数据权限限制） */
    private Boolean superAdmin;

    /** 权限标识清单；超管固定为单个通配符 {@code *} */
    private List<String> permissions;

    /** 已启用角色标识清单 */
    private List<String> roles;

    /**
     * 数据权限：是否可见全部数据（超管，或任一启用角色为「全部数据」）。
     *
     * <p>{@code null} 表示旧版本缓存里没有这一项 —— 按「不限制」处理，
     * 与改造前行为一致，避免滚动发布时新实例突然把所有人的可见范围清空。</p>
     */
    private Boolean dataScopeAll;

    /** 数据权限：是否包含「仅本人」档（任一启用角色为「仅本人」） */
    private Boolean dataScopeSelf;

    /** 数据权限：当前用户所属部门 ID，用于「仅本人」查看部门树时定位自己那一支 */
    private Long dataScopeDeptId;

    /** 数据权限：可见部门 ID 集合（已展开「本部门及以下」与「自定义部门」） */
    private List<Long> dataScopeDeptIds;

    /** 空快照：用户无任何角色 / 权限时使用，避免每次鉴权都回源数据库 */
    public static PermissionSnapshot empty() {
        return new PermissionSnapshot(false, List.of(), List.of(), false, false, null, List.of());
    }

    /**
     * 兼容三参数构造（调用方只关心权限时使用）。
     *
     * <p>数据权限按「不限制」处理：与改造前的行为一致，不会因为漏传而凭空收紧权限。</p>
     */
    public PermissionSnapshot(Boolean superAdmin, List<String> permissions, List<String> roles) {
        this(superAdmin, permissions, roles, true, false, null, List.of());
    }

    /** 是否不受数据权限限制；{@code null}（旧缓存）按不限制处理 */
    public boolean dataScopeUnrestricted() {
        return dataScopeAll == null || dataScopeAll;
    }

    /** 是否包含「仅本人」档；{@code null} 按不包含处理 */
    public boolean dataScopeHasSelf() {
        return Boolean.TRUE.equals(dataScopeSelf);
    }
}
