package com.designer.admin.system.mapper.rbac;

import com.designer.admin.common.constant.SystemConsts;
import com.designer.admin.system.model.entity.rbac.SysRole;
import com.designer.admin.system.model.entity.rbac.SysUserRole;
import com.github.yulichang.base.MPJBaseMapper;
import com.github.yulichang.toolkit.JoinWrappers;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
/**
 * 系统角色数据访问层。关联查询（join）改用 mpj Wrapper 在 service 层调用，不再写 SQL 注解。
 *
 * @author 雨夜终端
 */
@Mapper
public interface SysRoleMapper extends MPJBaseMapper<SysRole> {

    /**
     * 查询用户已启用的角色权限字符（{@code role_key}，去重）。
     *
     * @param userId 用户 ID
     * @return 角色权限字符列表
     */
    default List<String> selectRoleKeysByUserId(Long userId) {
        return selectJoinList(String.class, JoinWrappers.lambda(SysRole.class)
                .select(SysRole::getRoleKey)
                .distinct()
                .leftJoin(SysUserRole.class, SysUserRole::getRoleId, SysRole::getId)
                .eq(SysUserRole::getUserId, userId)
                .eq(SysRole::getStatus, SystemConsts.STATUS_ENABLE));
    }

    /**
     * 查询用户的全部角色 ID（含停用，用于角色分配回显）。
     *
     * @param userId 用户 ID
     * @return 角色 ID 列表
     */
    default List<Long> selectRoleIdsByUserId(Long userId) {
        return selectJoinList(Long.class, JoinWrappers.lambda(SysRole.class)
                .select(SysRole::getId)
                .leftJoin(SysUserRole.class, SysUserRole::getRoleId, SysRole::getId)
                .eq(SysUserRole::getUserId, userId));
    }

    /**
     * 判断用户是否为超级管理员：任一启用的角色 is_super=1 即视为超管。
     * 用 COUNT(*) 短路（命中即停）替代 EXISTS，避免方言差异。
     *
     * @param userId 用户 ID
     * @return 用户拥有任意启用超管角色时返回 {@code true}
     */
    default boolean selectIsSuperByUserId(Long userId) {
        Long cnt = selectJoinCount(JoinWrappers.lambda(SysRole.class)
                .leftJoin(SysUserRole.class, SysUserRole::getRoleId, SysRole::getId)
                .eq(SysUserRole::getUserId, userId)
                .eq(SysRole::getIsSuper, SystemConsts.IS_SUPER_YES)
                .eq(SysRole::getStatus, SystemConsts.STATUS_ENABLE));
        return cnt != null && cnt > 0;
    }

    /**
     * 统计角色标识占用数（只统计未删除的角色）。
     *
     * <p>唯一性由应用层保证（表上无唯一索引），逻辑删除的角色不再占位，
     * 因此查询必须走 {@code @TableLogic} 默认条件。</p>
     *
     * @param roleKey   角色标识
     * @param excludeId 需要排除的角色 ID，新增时传 {@code null}
     * @return 占用该标识的角色数
     */
    default Long countByRoleKey(String roleKey, Long excludeId) {
        return selectJoinCount(JoinWrappers.lambda(SysRole.class)
                .eq(SysRole::getRoleKey, roleKey)
                .ne(excludeId != null, SysRole::getId, excludeId));
    }

    /**
     * 查询用户全部启用角色的数据权限配置（只取 ID 与 {@code data_scope}）。
     *
     * <p><b>为什么和权限清单分开查</b>：权限清单是 {@code sys_menu.perms} 的字符串集合，
     * 数据权限需要的是角色上的一个编码 + 自定义部门关联，两者的 join 深度不同。
     * 拆开查各自只走一层关联，比一次 join 三张表再在内存里去重更直接。</p>
     *
     * <p>停用角色不参与数据权限计算 —— 与 {@link #selectRoleKeysByUserId} 口径一致：
     * 停用的角色既不给功能权限，也不给数据范围。</p>
     *
     * @param userId 用户 ID
     * @return 启用角色的精简列表（仅 {@code id} / {@code dataScope}）
     */
    default List<SysRole> selectEnabledDataScopesByUserId(Long userId) {
        return selectJoinList(SysRole.class, JoinWrappers.lambda(SysRole.class)
                .select(SysRole::getId, SysRole::getDataScope)
                .leftJoin(SysUserRole.class, SysUserRole::getRoleId, SysRole::getId)
                .eq(SysUserRole::getUserId, userId)
                .eq(SysRole::getStatus, SystemConsts.STATUS_ENABLE));
    }
}
