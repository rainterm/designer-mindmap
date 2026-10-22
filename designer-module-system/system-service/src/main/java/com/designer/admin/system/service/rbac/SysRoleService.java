package com.designer.admin.system.service.rbac;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.system.model.dto.rbac.RoleQueryDTO;
import com.designer.admin.system.model.dto.rbac.RoleSaveDTO;
import com.designer.admin.system.model.entity.rbac.SysRole;

import java.util.List;

/**
 * 角色服务
 *
 * @author 雨夜终端
 */
public interface SysRoleService {

    /**
     * 角色分页。
     *
     * @param query 分页与名称 / 编码 / 状态筛选条件
     * @return 角色分页结果
     */
    IPage<SysRole> listPage(RoleQueryDTO query);

    /**
     * 启用角色下拉。
     *
     * @return 角色列表
     */
    List<SysRole> listOptions();

    /**
     * 按主键查询角色，不存在时抛出业务异常。
     *
     * @param id 角色 ID
     * @return 角色
     */
    SysRole getById(Long id);

    /**
     * 新增角色并保存菜单权限。
     *
     * @param dto 保存参数
     */
    void add(RoleSaveDTO dto);

    /**
     * 修改角色并同步菜单权限。
     *
     * @param dto 保存参数
     */
    void update(RoleSaveDTO dto);

    /**
     * 批量删除角色；内置角色或已分配用户时拒绝。
     *
     * @param ids 角色 ID 列表
     */
    void remove(List<Long> ids);

    /**
     * 查询角色已分配的菜单 ID 列表。
     *
     * @param roleId 角色 ID
     * @return 菜单 ID 列表
     */
    List<Long> listMenuIds(Long roleId);

    /**
     * 查询角色自定义数据权限的部门 ID 列表（仅 {@code data_scope=2} 时有意义）。
     *
     * @param roleId 角色 ID
     * @return 部门 ID 列表，按升序
     */
    List<Long> listDeptIds(Long roleId);

    /**
     * 重新分配角色菜单（全量覆盖）。
     *
     * @param roleId  角色 ID
     * @param menuIds 菜单 ID 列表
     */
    void assignMenus(Long roleId, List<Long> menuIds);
}
