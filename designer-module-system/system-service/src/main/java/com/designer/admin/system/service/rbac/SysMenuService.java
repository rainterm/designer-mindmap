package com.designer.admin.system.service.rbac;

import com.designer.admin.system.model.dto.rbac.MenuSaveDTO;
import com.designer.admin.system.model.entity.rbac.SysMenu;
import com.designer.admin.system.model.vo.rbac.RouterVO;

import java.util.List;

/**
 * 菜单服务：CRUD + 动态路由树构建
 *
 * @author 雨夜终端
 */
public interface SysMenuService {

    /**
     * 为指定用户构建前端动态路由树。
     *
     * @param userId     用户 ID
     * @param superAdmin 是否超级管理员（是则返回全部菜单）
     * @return 路由树
     */
    List<RouterVO> listRouters(Long userId, boolean superAdmin);

    /**
     * 菜单树查询。
     *
     * @param keyword 菜单名称关键字，可空
     * @param status  状态过滤，可空
     * @return 菜单树
     */
    List<SysMenu> listTree(String keyword, Integer status);

    /**
     * 菜单树下拉选项。
     *
     * @return 菜单树
     */
    List<SysMenu> listOptions();

    /**
     * 按主键查询菜单，不存在时抛出业务异常。
     *
     * @param id 菜单 ID
     * @return 菜单
     */
    SysMenu getById(Long id);

    /**
     * 新增菜单。
     *
     * @param dto 保存参数
     */
    void add(MenuSaveDTO dto);

    /**
     * 修改菜单。
     *
     * @param dto 保存参数
     */
    void update(MenuSaveDTO dto);

    /**
     * 删除菜单及其全部子孙菜单，并清理角色关联。
     *
     * @param id 菜单 ID
     */
    void remove(Long id);

    /**
     * 查询角色已分配的菜单 ID 列表。
     *
     * @param roleId 角色 ID
     * @return 菜单 ID 列表
     */
    List<Long> listMenuIdsByRole(Long roleId);

    /**
     * 重新分配角色菜单（全量覆盖）。
     *
     * @param roleId  角色 ID
     * @param menuIds 菜单 ID 列表
     */
    void assignRoleMenus(Long roleId, List<Long> menuIds);
}
