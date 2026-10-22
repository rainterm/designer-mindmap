package com.designer.admin.system.spi;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.designer.admin.common.constant.DataScopeType;
import com.designer.admin.framework.mybatis.DataPermissionContext;
import com.designer.admin.framework.satoken.PermissionCache;
import com.designer.admin.framework.satoken.PermissionLoader;
import com.designer.admin.framework.satoken.PermissionSnapshot;
import com.designer.admin.system.mapper.rbac.SysDeptMapper;
import com.designer.admin.system.mapper.rbac.SysMenuMapper;
import com.designer.admin.system.mapper.rbac.SysRoleDeptMapper;
import com.designer.admin.system.mapper.rbac.SysRoleMapper;
import com.designer.admin.system.mapper.rbac.SysUserMapper;
import com.designer.admin.system.model.entity.rbac.SysRole;
import com.designer.admin.system.model.entity.rbac.SysRoleDept;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 权限数据源：对接 sys_menu / sys_role / sys_role_dept / sys_dept，并通过 {@link PermissionCache} 做读缓存。
 *
 * <p><b>缓存放置位置的理由</b>：{@code @SaCheckPermission} 每个请求至少调一次本类，
 * 非超管原本要 2 次 SQL（isSuper + perms）。缓存后命中即 1 次 Redis GET，
 * 回源仍是 4~5 次 SQL（权限、角色、部门、自定义部门、部门子树），但只在缓存失效或 TTL 到期时发生。</p>
 *
 * <p><b>无权限用户也要缓存空快照</b>：否则一个没有任何角色的账号
 * （或恶意注册的账号）每次请求仍会打 2 次 SQL，缓存等于没生效。</p>
 *
 * @author 雨夜终端
 */
@Component
public class SysPermissionLoader implements PermissionLoader {

    @Db
    private SysMenuMapper menuMapper;

    @Db
    private SysRoleMapper roleMapper;

    @Db
    private SysRoleDeptMapper roleDeptMapper;

    @Db
    private SysDeptMapper deptMapper;

    @Db
    private SysUserMapper userMapper;

    @Inject
    private PermissionCache permissionCache;

    @Override
    public PermissionSnapshot load(Long userId) {
        if (userId == null) {
            return PermissionSnapshot.empty();
        }
        // 装载权限本身必须绕开数据权限：否则「查快照 → 查 sys_user → 又要查快照」会无限递归，
        // 而且被过滤后的 sys_user 可能查不到自己的 dept_id，快照就此固化成一个错误的空范围
        return DataPermissionContext.ignore(() -> doLoad(userId));
    }

    private PermissionSnapshot doLoad(Long userId) {
        PermissionSnapshot cached = permissionCache.get(userId);
        if (cached != null) {
            return cached;
        }

        PermissionSnapshot snapshot = query(userId);
        permissionCache.put(userId, snapshot);
        return snapshot;
    }

    /**
     * 回源装载。
     *
     * <p>超管跳过权限清单查询：它必然拥有全部权限，前端与 Sa-Token 都认通配符 {@code *}，
     * 这时候再去 join 三张表取一堆 perms 是纯浪费。数据权限同理 —— 超管不受限制。</p>
     */
    private PermissionSnapshot query(Long userId) {
        boolean superAdmin = roleMapper.selectIsSuperByUserId(userId);
        List<String> roles = List.copyOf(roleMapper.selectRoleKeysByUserId(userId));
        List<String> permissions = superAdmin
                ? List.of("*")
                : List.copyOf(menuMapper.selectPermsByUserId(userId));

        Long deptId = superAdmin ? null : userMapper.selectDeptIdById(userId);
        DataScopePart dataScope = superAdmin
                ? DataScopePart.ALL
                : resolveDataScope(userId, deptId);

        return new PermissionSnapshot(superAdmin, permissions, roles,
                dataScope.all(), dataScope.self(), deptId, dataScope.deptIds());
    }

    /**
     * 把「角色上的 data_scope」翻译成「可见部门 ID 集合 + 是否含本人」。
     *
     * <p><b>多角色取并集</b>：任一角色为「全部数据」即不受限；否则合并各角色的部门集合，
     * 任一角色为「仅本人」就额外放行本人记录。加角色只会放大可见范围，不会意外缩小。</p>
     *
     * <p><b>部门子树在这里展开</b>：「本部门及以下」在回源时就把整棵子树算成一个 ID 集合，
     * 而不是每次查询都去查部门树。代价是部门树变更后必须让相关快照失效
     * （见 {@code PermissionInvalidator#evictAllUsers}）。</p>
     *
     * <p><b>无法识别的编码按「无可见范围」处理</b>：宁可少看到数据，也不能因为一个脏编码
     * 把可见范围放大成全部。这类情况只可能来自绕过应用层的直连改库。</p>
     *
     * @param userId   用户 ID
     * @param userDeptId 用户所属部门 ID，可为 {@code null}
     * @return 数据范围
     */
    private DataScopePart resolveDataScope(Long userId, Long userDeptId) {
        List<SysRole> roleScopes = roleMapper.selectEnabledDataScopesByUserId(userId);
        if (roleScopes.isEmpty()) {
            return DataScopePart.NONE;
        }

        boolean all = false;
        boolean self = false;
        Set<Long> deptIds = new LinkedHashSet<>();
        List<Long> customRoleIds = new ArrayList<>();

        for (SysRole role : roleScopes) {
            DataScopeType type = DataScopeType.fromCode(role.getDataScope());
            if (type == null) {
                continue;
            }
            switch (type) {
                case ALL -> all = true;
                case SELF -> self = true;
                case CUSTOM -> customRoleIds.add(role.getId());
                case DEPT -> {
                    if (userDeptId != null) {
                        deptIds.add(userDeptId);
                    }
                }
                case DEPT_AND_CHILD -> {
                    if (userDeptId != null) {
                        deptIds.addAll(deptMapper.selectSelfAndDescendantIds(userDeptId));
                    }
                }
                default -> {
                    // 枚举已穷举，这里的 default 只为满足编译器；新增档位时应显式处理
                }
            }
        }

        if (all) {
            return DataScopePart.ALL;
        }
        if (!customRoleIds.isEmpty()) {
            deptIds.addAll(roleDeptMapper.selectList(new LambdaQueryWrapper<SysRoleDept>()
                            .in(SysRoleDept::getRoleId, customRoleIds))
                    .stream()
                    .map(SysRoleDept::getDeptId)
                    .toList());
        }
        return new DataScopePart(false, self, List.copyOf(deptIds));
    }

    /**
     * 数据范围的装载结果（仅本类使用的中间结构）。
     *
     * @param all     是否可见全部
     * @param self    是否包含「仅本人」
     * @param deptIds 可见部门 ID 集合
     */
    private record DataScopePart(boolean all, boolean self, List<Long> deptIds) {

        /** 不受限制 */
        private static final DataScopePart ALL = new DataScopePart(true, false, List.of());

        /** 什么都看不到（无启用角色 / 角色编码非法） */
        private static final DataScopePart NONE = new DataScopePart(false, false, List.of());
    }
}
