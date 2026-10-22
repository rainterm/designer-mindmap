package com.designer.admin.system.service.rbac.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.common.constant.DataScopeType;
import com.designer.admin.common.constant.SystemConsts;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.framework.mybatis.DataPermissionGuard;
import com.designer.admin.system.mapper.rbac.SysDeptMapper;
import com.designer.admin.system.mapper.rbac.SysRoleDeptMapper;
import com.designer.admin.system.mapper.rbac.SysRoleMapper;
import com.designer.admin.system.mapper.rbac.SysUserRoleMapper;
import com.designer.admin.system.model.dto.rbac.RoleQueryDTO;
import com.designer.admin.system.model.dto.rbac.RoleSaveDTO;
import com.designer.admin.system.model.entity.rbac.SysDept;
import com.designer.admin.system.model.entity.rbac.SysRole;
import com.designer.admin.system.model.entity.rbac.SysRoleDept;
import com.designer.admin.system.model.entity.rbac.SysUserRole;
import com.designer.admin.system.service.rbac.SysMenuService;
import com.designer.admin.system.service.rbac.SysRoleService;
import com.designer.admin.system.spi.PermissionInvalidator;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.data.annotation.Transaction;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 角色服务
 *
 * @author 雨夜终端
 */
@Component
public class SysRoleServiceImpl implements SysRoleService {

    @Db
    private SysRoleMapper roleMapper;
    @Db
    private SysUserRoleMapper userRoleMapper;
    @Db
    private SysRoleDeptMapper roleDeptMapper;
    @Db
    private SysDeptMapper deptMapper;
    @Inject
    private SysMenuService menuService;
    @Inject
    private PermissionInvalidator permissionInvalidator;
    @Inject
    private DataPermissionGuard dataPermissionGuard;

    @Override
    public IPage<SysRole> listPage(RoleQueryDTO query) {
        LambdaQueryWrapper<SysRole> w = new LambdaQueryWrapper<SysRole>()
                .like(notBlank(query.getRoleName()), SysRole::getRoleName, query.getRoleName())
                .like(notBlank(query.getRoleKey()), SysRole::getRoleKey, query.getRoleKey())
                .eq(query.getStatus() != null, SysRole::getStatus, query.getStatus())
                .orderByAsc(SysRole::getSort);
        return roleMapper.selectPage(query.toPage(), w);
    }

    @Override
    public List<SysRole> listOptions() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getStatus, 1)
                .orderByAsc(SysRole::getSort));
    }

    @Override
    public SysRole getById(Long id) {
        return roleMapper.selectById(id);
    }

    @Override
    @Transaction
    public void add(RoleSaveDTO dto) {
        Long dup = roleMapper.countByRoleKey(dto.getRoleKey(), null);
        if (dup != null && dup > 0) {
            throw new BizException("角色标识已存在");
        }
        SysRole role = toEntity(dto);
        if (role.getStatus() == null) {
            role.setStatus(SystemConsts.STATUS_ENABLE);
        }
        if (role.getSort() == null) {
            role.setSort(0);
        }
        // 新增角色默认自定义；不允许通过 DTO 创建内置/超管角色
        role.setRoleType(SystemConsts.ROLE_TYPE_CUSTOM);
        role.setIsSuper(SystemConsts.IS_SUPER_NO);
        // 未显式指定数据权限时按「全部数据」，与改造前「没有数据权限限制」的行为一致
        if (role.getDataScope() == null) {
            role.setDataScope(DataScopeType.ALL.getCode());
        }
        DataScopeType scope = requireValidScope(role.getDataScope());
        roleMapper.insert(role);
        saveRoleDepts(role.getId(), scope, dto.getDeptIds() == null ? List.of() : dto.getDeptIds());
        if (dto.getMenuIds() != null) {
            menuService.assignRoleMenus(role.getId(), dto.getMenuIds());
        }
    }

    @Override
    @Transaction
    public void update(RoleSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("角色ID不能为空");
        }
        SysRole existing = roleMapper.selectById(dto.getId());
        if (existing == null) {
            throw new BizException("角色不存在");
        }
        if (Integer.valueOf(SystemConsts.ROLE_TYPE_BUILTIN).equals(existing.getRoleType())) {
            throw new BizException("内置角色不允许修改");
        }
        Long dup = roleMapper.countByRoleKey(dto.getRoleKey(), dto.getId());
        if (dup != null && dup > 0) {
            throw new BizException("角色标识已存在");
        }
        SysRole update = toEntity(dto);        // 不允许通过 DTO 篡改超管/类型标记
        update.setIsSuper(existing.getIsSuper());
        update.setRoleType(existing.getRoleType());
        // 数据权限：未传表示不改（updateById 会跳过 null 字段）
        DataScopeType newScope = null;
        if (update.getDataScope() != null) {
            newScope = requireValidScope(update.getDataScope());
        }
        roleMapper.updateById(update);
        if (newScope != null) {
            saveRoleDepts(dto.getId(), newScope, dto.getDeptIds());
        }
        if (dto.getMenuIds() != null) {
            menuService.assignRoleMenus(dto.getId(), dto.getMenuIds());
        }
        // 角色标识 / 状态 / 数据权限变了才需要整体失效；只改组名、备注时无需动缓存。
        // 数据权限存在快照里，改了就必须删 —— 否则用户仍按旧范围看数据，最长持续一个 TTL
        boolean identityChanged = !Objects.equals(existing.getRoleKey(), update.getRoleKey())
                || !Objects.equals(existing.getStatus(), update.getStatus())
                || (newScope != null && !Objects.equals(existing.getDataScope(), newScope.getCode()));
        if (identityChanged || dto.getMenuIds() == null) {
            permissionInvalidator.evictRole(dto.getId());
        }
    }

    @Override
    @Transaction
    public void remove(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            SysRole role = roleMapper.selectById(id);
            if (role == null) {
                continue;
            }
            if (Integer.valueOf(SystemConsts.ROLE_TYPE_BUILTIN).equals(role.getRoleType())) {
                throw new BizException("角色【" + role.getRoleName() + "】为内置角色，不允许删除");
            }
            Long bound = userRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRole>()
                    .eq(SysUserRole::getRoleId, id));
            if (bound != null && bound > 0) {
                throw new BizException("角色【" + role.getRoleName() + "】已分配用户，无法删除");
            }
        }
        roleMapper.deleteByIds(ids);
        for (Long id : ids) {
            roleDeptMapper.delete(new LambdaQueryWrapper<SysRoleDept>().eq(SysRoleDept::getRoleId, id));
        }
        // 正常路径下没有用户绑定（上面已校验），这里仍删一次：防止历史脏数据残留在缓存里
        permissionInvalidator.evictRoles(ids);
    }

    @Override
    public List<Long> listMenuIds(Long roleId) {
        return menuService.listMenuIdsByRole(roleId);
    }

    @Override
    public List<Long> listDeptIds(Long roleId) {
        return roleDeptMapper.selectList(new LambdaQueryWrapper<SysRoleDept>()
                        .eq(SysRoleDept::getRoleId, roleId))
                .stream()
                .map(SysRoleDept::getDeptId)
                .sorted()
                .toList();
    }

    @Override
    @Transaction
    public void assignMenus(Long roleId, List<Long> menuIds) {
        assertNotSuperRole(roleId);
        menuService.assignRoleMenus(roleId, menuIds);
    }

    private void assertNotSuperRole(Long roleId) {
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BizException("角色不存在");
        }
        if (Integer.valueOf(SystemConsts.IS_SUPER_YES).equals(role.getIsSuper())) {
            throw new BizException("超级管理员默认拥有全部权限，无需分配菜单");
        }
    }

    private SysRole toEntity(RoleSaveDTO dto) {
        SysRole role = new SysRole();
        role.setId(dto.getId());
        role.setRoleName(dto.getRoleName());
        role.setRoleKey(dto.getRoleKey());
        role.setSort(dto.getSort());
        role.setStatus(dto.getStatus());
        role.setRoleType(dto.getRoleType());
        role.setDataScope(dto.getDataScope());
        role.setRemark(dto.getRemark());
        return role;
    }

    /**
     * 解析并校验数据权限编码。
     *
     * @param code 编码
     * @return 枚举
     * @throws BizException 编码为空或不在 1~5 之间
     */
    private DataScopeType requireValidScope(Integer code) {
        DataScopeType type = DataScopeType.fromCode(code);
        if (type == null) {
            throw new BizException("数据权限范围不合法");
        }
        return type;
    }

    /**
     * 重写角色的自定义部门集合。
     *
     * <p><b>非自定义范围也要清空关联</b>：否则「自定义勾了 A、切成本部门、再切回自定义」
     * 会把 A 凭空带回来 —— 中间那次保存看起来生效了，实际旧授权一直躺在表里。</p>
     *
     * <p><b>非超管不能越权授权</b>：{@code system:role:edit} 本身是高危权限，
     * 但仍不应允许它把一个角色配成「全部数据」或勾选自己看不到的部门 ——
     * 那等于给自己（或同角色的人）开了一扇直接看全公司的门。
     * 超管不受限制。</p>
     *
     * @param roleId  角色 ID
     * @param scope   数据范围
     * @param deptIds 自定义部门（仅 {@link DataScopeType#CUSTOM} 有效）；{@code null} 表示保留原有关联
     */
    private void saveRoleDepts(Long roleId, DataScopeType scope, List<Long> deptIds) {
        assertGrantable(scope, deptIds);
        if (scope != DataScopeType.CUSTOM) {
            roleDeptMapper.delete(new LambdaQueryWrapper<SysRoleDept>().eq(SysRoleDept::getRoleId, roleId));
            return;
        }
        // null = 调用方没带部门（如只改状态的局部更新），保留原有勾选，避免误判成「清空」
        if (deptIds == null) {
            return;
        }
        roleDeptMapper.delete(new LambdaQueryWrapper<SysRoleDept>().eq(SysRoleDept::getRoleId, roleId));
        Set<Long> ids = distinct(deptIds);
        if (ids.isEmpty()) {
            throw new BizException("自定义数据权限至少需要选择一个部门");
        }
        List<Long> existing = deptMapper.selectBatchIds(ids).stream().map(SysDept::getId).toList();
        if (existing.size() != ids.size()) {
            throw new BizException("所选部门中包含不存在的部门，请刷新后重试");
        }
        for (Long deptId : ids) {
            SysRoleDept link = new SysRoleDept();
            link.setRoleId(roleId);
            link.setDeptId(deptId);
            roleDeptMapper.insert(link);
        }
    }

    /**
     * 授权边界校验：非超管只能授予自己数据范围内的配置。
     *
     * @param scope   拟授予的数据范围
     * @param deptIds 拟授予的自定义部门
     */
    private void assertGrantable(DataScopeType scope, List<Long> deptIds) {
        if (dataPermissionGuard.isUnrestricted()) {
            return;
        }
        if (scope == DataScopeType.ALL) {
            throw new BizException(403, "只能授予不超过自身数据范围的数据权限");
        }
        for (Long deptId : distinct(deptIds)) {
            if (!dataPermissionGuard.canSeeDept(deptId)) {
                throw new BizException(403, "只能授予自身数据范围内的部门");
            }
        }
    }

    private static Set<Long> distinct(List<Long> ids) {
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

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
