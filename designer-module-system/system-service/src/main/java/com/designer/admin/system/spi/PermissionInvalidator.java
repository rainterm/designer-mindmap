package com.designer.admin.system.spi;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.designer.admin.framework.satoken.PermissionCache;
import com.designer.admin.system.mapper.rbac.SysRoleMenuMapper;
import com.designer.admin.system.mapper.rbac.SysUserRoleMapper;
import com.designer.admin.system.model.entity.rbac.SysRoleMenu;
import com.designer.admin.system.model.entity.rbac.SysUserRole;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.data.tran.TranListener;
import org.noear.solon.data.tran.TranUtils;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 权限缓存失效器：把「角色 / 菜单变更」翻译成「哪些用户的权限快照需要删除」。
 *
 * <p><b>为什么不能让写侧各自删</b>：权限的来源是
 * {@code 用户 → 角色 → 菜单}，一次角色修改影响的是一批用户，而写侧通常只拿到角色 ID。
 * 解析「角色 → 用户」「菜单 → 角色 → 用户」的 join 只应该有一处实现，
 * 否则漏掉一个入口就是一类越权/权限不生效问题。</p>
 *
 * <p><b>为什么在事务提交后才删</b>：如果事务内先删缓存，另一个请求立刻回源数据库，
 * 读到的还是未提交的旧数据，并把它重新写回缓存；等提交完成，缓存里留下的仍是旧权限。
 * 所以用户集合在事务内解析（此时数据还看得见），删除动作挂到 {@code afterCommit}；
 * 事务回滚时不执行，旧缓存继续有效 —— 与数据库保持一致。</p>
 *
 * <p><b>删失败不抛异常</b>：{@link PermissionCache#evict} 自己吞掉 Redis 异常。
 * 权限修改成功但缓存删失败时，旧值最多残留一个 TTL，这比让整个角色修改失败更可接受。</p>
 *
 * @author 雨夜终端
 */
@Component
public class PermissionInvalidator {

    @Db
    private SysUserRoleMapper userRoleMapper;

    @Db
    private SysRoleMenuMapper roleMenuMapper;

    @Inject
    private PermissionCache permissionCache;

    /**
     * 失效单个用户（用户 ↔ 角色关系变化时调用）。
     *
     * @param userId 用户 ID
     */
    public void evictUser(Long userId) {
        if (userId != null) {
            evictUsers(List.of(userId));
        }
    }

    /**
     * 失效一批用户。
     *
     * @param userIds 用户 ID 集合，允许为空
     */
    public void evictUsers(Collection<Long> userIds) {
        Set<Long> ids = distinct(userIds);
        if (ids.isEmpty()) {
            return;
        }
        afterCommit(() -> permissionCache.evict(ids));
    }

    /**
     * 失效一个角色下的全部用户（角色本身变化：标识、状态、菜单分配）。
     *
     * @param roleId 角色 ID
     */
    public void evictRole(Long roleId) {
        if (roleId != null) {
            evictRoles(List.of(roleId));
        }
    }

    /**
     * 失效一批角色下的全部用户。
     *
     * @param roleIds 角色 ID 集合，允许为空
     */
    public void evictRoles(Collection<Long> roleIds) {
        Set<Long> ids = distinct(roleIds);
        if (ids.isEmpty()) {
            return;
        }
        // 在事务内解析：调用方可能紧接着删除关联行（如菜单删除会清理 sys_role_menu）
        List<Long> userIds = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .in(SysUserRole::getRoleId, ids)
                        .select(SysUserRole::getUserId))
                .stream()
                .map(SysUserRole::getUserId)
                .toList();
        evictUsers(userIds);
    }

    /**
     * 失效受某个菜单影响的用户（菜单的 perms / status 变化、菜单被删除）。
     *
     * @param menuId 菜单 ID
     */
    public void evictMenu(Long menuId) {
        if (menuId != null) {
            evictMenus(List.of(menuId));
        }
    }

    /**
     * 失效受一批菜单影响的用户。
     *
     * @param menuIds 菜单 ID 集合，允许为空
     */
    public void evictMenus(Collection<Long> menuIds) {
        Set<Long> ids = distinct(menuIds);
        if (ids.isEmpty()) {
            return;
        }
        // 与 evictRoles 同理：必须先于调用方清理 sys_role_menu 执行
        List<Long> roleIds = roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                        .in(SysRoleMenu::getMenuId, ids)
                        .select(SysRoleMenu::getRoleId))
                .stream()
                .map(SysRoleMenu::getRoleId)
                .toList();
        evictRoles(roleIds);
    }

    /**
     * 失效全部用户的权限快照（部门树结构变化时调用）。
     *
     * <p><b>为什么部门变动要清所有人</b>：「本部门及以下」的可见部门集合在装载快照时
     * 就已展开成 ID 集合。部门新增、移动、删除都会让这个集合失真：
     * 移动一支子树后，旧快照可能仍然包含「已经不该看到」的部门 —— 那是越权，不是缓存陈旧。
     * 精确解析「哪些用户受影响」需要把每个角色的数据范围与部门子树做交集，
     * 复杂度远超收益；部门调整是低频操作，一次全量失效更简单也更安全。</p>
     *
     * <p>只清有角色的用户：没有角色的用户本来就没有任何权限与数据范围。</p>
     */
    public void evictAllUsers() {
        List<Long> userIds = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .select(SysUserRole::getUserId))
                .stream()
                .map(SysUserRole::getUserId)
                .toList();
        evictUsers(userIds);
    }

    /**
     * 事务内挂到提交之后，事务外立即执行。
     *
     * <p>没有事务时立即删是安全的：调用方已经完成写入（本类的调用点都在写完数据之后），
     * 不会出现「删了缓存但数据还没落库」的窗口。</p>
     */
    private void afterCommit(Runnable action) {
        if (TranUtils.inTrans()) {
            TranUtils.listen(new TranListener() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    private static Set<Long> distinct(Collection<Long> ids) {
        Set<Long> result = new LinkedHashSet<>();
        if (ids != null) {
            for (Long id : ids) {
                if (id != null) {
                    result.add(id);
                }
            }
        }
        return result;
    }
}
