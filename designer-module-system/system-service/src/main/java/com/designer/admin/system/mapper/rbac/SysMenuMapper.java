package com.designer.admin.system.mapper.rbac;

import com.designer.admin.common.constant.SystemConsts;
import com.designer.admin.system.model.entity.rbac.SysMenu;
import com.designer.admin.system.model.entity.rbac.SysRole;
import com.designer.admin.system.model.entity.rbac.SysRoleMenu;
import com.designer.admin.system.model.entity.rbac.SysUserRole;
import com.github.yulichang.base.MPJBaseMapper;
import com.github.yulichang.toolkit.JoinWrappers;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 系统菜单数据访问层。关联查询（join）改用 mpj Wrapper 在 service 层调用，不再写 SQL 注解。
 *
 * @author 雨夜终端
 */
@Mapper
public interface SysMenuMapper extends MPJBaseMapper<SysMenu> {

    /**
     * 查询用户的所有按钮权限标识（启用 + role 启用 + perms 非空），
     * 供前端 {@code v-perm} 与后端 {@code @SaCheckPermission} 使用。
     *
     * @param userId 用户 ID
     * @return 权限标识列表
     */
    default List<String> selectPermsByUserId(Long userId) {
        return selectJoinList(String.class, JoinWrappers.lambda(SysMenu.class)
                .select(SysMenu::getPerms)
                .distinct()
                .leftJoin(SysRoleMenu.class, SysRoleMenu::getMenuId, SysMenu::getId)
                .leftJoin(SysUserRole.class, SysUserRole::getRoleId, SysRoleMenu::getRoleId)
                .leftJoin(SysRole.class, SysRole::getId, SysUserRole::getRoleId)
                .eq(SysUserRole::getUserId, userId)
                .eq(SysRole::getStatus, SystemConsts.STATUS_ENABLE)
                .eq(SysMenu::getStatus, SystemConsts.STATUS_ENABLE)
                .isNotNull(SysMenu::getPerms)
                .ne(SysMenu::getPerms, ""));
    }

    /**
     * 查询用户可见的目录（M）+ 菜单（C），用于构建前端动态路由树。
     *
     * @param userId 用户 ID
     * @return 可见菜单列表
     */
    default List<SysMenu> selectRouterMenusByUserId(Long userId) {
        return selectJoinList(SysMenu.class, JoinWrappers.lambda(SysMenu.class)
                .selectAll(SysMenu.class)
                .distinct()
                .leftJoin(SysRoleMenu.class, SysRoleMenu::getMenuId, SysMenu::getId)
                .leftJoin(SysUserRole.class, SysUserRole::getRoleId, SysRoleMenu::getRoleId)
                .leftJoin(SysRole.class, SysRole::getId, SysUserRole::getRoleId)
                .eq(SysUserRole::getUserId, userId)
                .eq(SysRole::getStatus, SystemConsts.STATUS_ENABLE)
                .eq(SysMenu::getStatus, SystemConsts.STATUS_ENABLE)
                .eq(SysMenu::getVisible, 1)
                .in(SysMenu::getMenuType, SystemConsts.MENU_TYPE_DIR, SystemConsts.MENU_TYPE_MENU)
                .orderByAsc(SysMenu::getParentId)
                .orderByAsc(SysMenu::getSort));
    }
}