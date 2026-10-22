package com.designer.admin.system.service.rbac.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.designer.admin.common.constant.SystemConsts;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.common.util.TreeUtils;
import com.designer.admin.common.constant.HttpConsts;
import com.designer.admin.system.constant.SysMenuConsts;
import com.designer.admin.system.mapper.rbac.SysMenuMapper;
import com.designer.admin.system.mapper.rbac.SysRoleMenuMapper;
import com.designer.admin.system.model.dto.rbac.MenuSaveDTO;
import com.designer.admin.system.model.entity.rbac.SysMenu;
import com.designer.admin.system.model.entity.rbac.SysRoleMenu;
import com.designer.admin.system.model.vo.rbac.RouterVO;
import com.designer.admin.system.service.rbac.SysMenuService;
import com.designer.admin.system.spi.PermissionInvalidator;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.data.annotation.Transaction;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 菜单服务：CRUD + 动态路由树构建
 *
 * @author 雨夜终端
 */
@Component
public class SysMenuServiceImpl implements SysMenuService {

    @Db
    private SysMenuMapper menuMapper;
    @Db
    private SysRoleMenuMapper roleMenuMapper;
    @Inject
    private PermissionInvalidator permissionInvalidator;

    @Override
    public List<RouterVO> listRouters(Long userId, boolean superAdmin) {
        List<SysMenu> menus;
        if (superAdmin) {
            menus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                    .eq(SysMenu::getStatus, SystemConsts.STATUS_ENABLE)
                    .ne(SysMenu::getMenuType, SystemConsts.MENU_TYPE_BUTTON)
                    .orderByAsc(SysMenu::getParentId).orderByAsc(SysMenu::getSort));
        } else {
            menus = menuMapper.selectRouterMenusByUserId(userId).stream()
                    .filter(m -> !SystemConsts.MENU_TYPE_BUTTON.equals(m.getMenuType()))
                    .sorted(Comparator.comparing(SysMenu::getParentId).thenComparing(SysMenu::getSort))
                    .toList();
        }

        List<RouterVO> nodes = menus.stream().map(this::toRouter).toList();
        return TreeUtils.buildTree(nodes, 0L);
    }

    private RouterVO toRouter(SysMenu menu) {
        RouterVO router = new RouterVO();
        router.setId(menu.getId());
        router.setParentId(menu.getParentId());
        router.setPath(menu.getPath());
        router.setName(buildRouteName(menu));
        router.setComponent(SystemConsts.MENU_TYPE_DIR.equals(menu.getMenuType()) ? SysMenuConsts.LAYOUT_COMPONENT : menu.getComponent());
        RouterVO.MetaVO meta = new RouterVO.MetaVO();
        meta.setTitle(menu.getMenuName());
        meta.setIcon(menu.getIcon());
        meta.setKeepAlive(false);
        // visible=0：详情/编辑等跳转页，注册路由但不进侧栏
        meta.setHidden(menu.getVisible() == null || menu.getVisible() != SystemConsts.STATUS_ENABLE);
        String component = menu.getComponent();
        String path = menu.getPath();
        String linkUrl = resolveLinkUrl(component, path);
        boolean ssoExternal = component != null
                && component.regionMatches(true, 0, SysMenuConsts.COMPONENT_EXTERNAL, 0, SysMenuConsts.COMPONENT_EXTERNAL.length());
        boolean external = linkUrl != null || ssoExternal;
        meta.setExternal(external);
        if (linkUrl != null) {
            meta.setLinkUrl(linkUrl);
        }
        if (ssoExternal) {
            String ssoClient = "";
            int colon = component.indexOf(':');
            if (colon >= 0 && colon < component.length() - 1) {
                String key = component.substring(colon + 1).trim();
                if (!key.isEmpty()) {
                    ssoClient = key;
                }
            }
            meta.setSsoClient(ssoClient);
        }
        router.setMeta(meta);
        router.setChildren(new ArrayList<>());
        return router;
    }

    /**
     * 解析普通外链地址：
     * - component = Link:https://... → 取冒号后 URL
     * - component = Link 且 path 为 http(s) → 用 path
     * - path 本身为 http(s) → 用 path
     */
    private String resolveLinkUrl(String component, String path) {
        if (component != null && component.regionMatches(true, 0, SysMenuConsts.COMPONENT_LINK, 0, SysMenuConsts.COMPONENT_LINK.length())) {
            int colon = component.indexOf(':');
            if (colon >= 0 && colon < component.length() - 1) {
                String url = component.substring(colon + 1).trim();
                if (!url.isEmpty()) {
                    return url;
                }
            }
        }
        if (isExternalUrl(path)) {
            return path;
        }
        return null;
    }

    /**
     * 判断路径是否为 http(s) 外链。
     *
     * @param path 路径
     * @return 是外链时返回 {@code true}
     */
    private boolean isExternalUrl(String path) {
        return path != null && (path.startsWith(HttpConsts.PREFIX_HTTP) || path.startsWith(HttpConsts.PREFIX_HTTPS));
    }

    /** 由路径生成唯一 PascalCase 路由名：/system/user -> SystemUser */
    private String buildRouteName(SysMenu menu) {
        String path = menu.getPath();
        if (path == null || path.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String seg : path.replaceAll(SysMenuConsts.TRIM_SLASH_REGEX, "").split(HttpConsts.PATH_SEPARATOR)) {
            if (seg.isEmpty()) {
                continue;
            }
            sb.append(Character.toUpperCase(seg.charAt(0))).append(seg.substring(1));
        }
        return sb.toString();
    }

    @Override
    public List<SysMenu> listTree(String keyword, Integer status) {
        LambdaQueryWrapper<SysMenu> w = new LambdaQueryWrapper<SysMenu>()
                .like(keyword != null && !keyword.isEmpty(), SysMenu::getMenuName, keyword)
                .eq(status != null, SysMenu::getStatus, status)
                .orderByAsc(SysMenu::getParentId).orderByAsc(SysMenu::getSort);
        return TreeUtils.buildTree(menuMapper.selectList(w), 0L);
    }

    @Override
    public List<SysMenu> listOptions() {
        return TreeUtils.buildTree(menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .ne(SysMenu::getMenuType, SystemConsts.MENU_TYPE_BUTTON)
                .orderByAsc(SysMenu::getParentId).orderByAsc(SysMenu::getSort)), 0L);
    }

    @Override
    public SysMenu getById(Long id) {
        return menuMapper.selectById(id);
    }

    @Override
    public void add(MenuSaveDTO dto) {
        SysMenu menu = toEntity(dto);
        if (menu.getParentId() == null) {
            menu.setParentId(0L);
        }
        menuMapper.insert(menu);
        adjustParentType(menu);
    }

    @Override
    @Transaction
    public void update(MenuSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("菜单ID不能为空");
        }
        menuMapper.updateById(toEntity(dto));
        adjustParentType(toEntity(dto));
        // perms / status 都可能变，受该菜单影响的用户权限快照全部作废
        permissionInvalidator.evictMenu(dto.getId());
    }

    private SysMenu toEntity(MenuSaveDTO dto) {
        SysMenu menu = new SysMenu();
        menu.setId(dto.getId());
        menu.setParentId(dto.getParentId());
        menu.setMenuName(dto.getMenuName());
        menu.setMenuType(dto.getMenuType());
        menu.setPath(dto.getPath());
        menu.setComponent(dto.getComponent());
        menu.setPerms(dto.getPerms());
        menu.setIcon(dto.getIcon());
        menu.setSort(dto.getSort());
        menu.setVisible(dto.getVisible());
        menu.setStatus(dto.getStatus());
        return menu;
    }

    /** 子节点存在时，父节点应转为目录 */
    private void adjustParentType(SysMenu menu) {
        if (menu.getParentId() != null && menu.getParentId() != 0L && menu.getId() != null) {
            SysMenu parent = menuMapper.selectById(menu.getParentId());
            if (parent != null && !SystemConsts.MENU_TYPE_DIR.equals(parent.getMenuType())) {
                SysMenu fix = new SysMenu();
                fix.setId(parent.getId());
                fix.setMenuType(SystemConsts.MENU_TYPE_DIR);
                menuMapper.updateById(fix);
            }
        }
    }

    @Override
    @Transaction
    public void remove(Long id) {
        List<Long> ids = collectDescendantIds(id);
        ids.add(id);
        // 必须在删除 sys_role_menu 之前解析受影响用户，否则关系行没了就找不到人
        permissionInvalidator.evictMenus(ids);
        menuMapper.deleteByIds(ids);
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().in(SysRoleMenu::getMenuId, ids));
    }

    /** 收集全部子孙菜单 id（不含自身） */
    private List<Long> collectDescendantIds(Long parentId) {
        List<Long> result = new ArrayList<>();
        Deque<Long> queue = new ArrayDeque<>();
        queue.add(parentId);
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            List<SysMenu> children = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                    .eq(SysMenu::getParentId, current)
                    .select(SysMenu::getId));
            for (SysMenu child : children) {
                if (child.getId() == null) {
                    continue;
                }
                result.add(child.getId());
                queue.add(child.getId());
            }
        }
        return result;
    }

    @Override
    public List<Long> listMenuIdsByRole(Long roleId) {
        return roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                .eq(SysRoleMenu::getRoleId, roleId))
                .stream()
                .map(SysRoleMenu::getMenuId)
                .toList();
    }

    @Override
    public void assignRoleMenus(Long roleId, List<Long> menuIds) {
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId));
        Set<Long> ids = expandWithHiddenMenus(menuIds);
        for (Long menuId : ids) {
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(roleId);
            rm.setMenuId(menuId);
            roleMenuMapper.insert(rm);
        }
        // 角色菜单变了，该角色下所有用户的权限快照作废
        permissionInvalidator.evictRole(roleId);
    }

    /**
     * 隐藏页（visible=0）不单独勾选：按已选菜单的权限标识 / 父级自动带上。
     */
    private Set<Long> expandWithHiddenMenus(List<Long> menuIds) {
        Set<Long> ids = new LinkedHashSet<>();
        if (menuIds != null) {
            menuIds.stream().filter(Objects::nonNull).forEach(ids::add);
        }
        if (ids.isEmpty()) {
            return ids;
        }
        List<SysMenu> all = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getStatus, SystemConsts.STATUS_ENABLE));
        Set<String> grantedPerms = all.stream()
                .filter(m -> ids.contains(m.getId()))
                .map(SysMenu::getPerms)
                .filter(p -> p != null && !p.isBlank())
                .collect(Collectors.toCollection(HashSet::new));
        for (SysMenu menu : all) {
            if (menu.getVisible() != null && menu.getVisible() == SystemConsts.STATUS_ENABLE) {
                continue;
            }
            boolean byParent = menu.getParentId() != null && ids.contains(menu.getParentId());
            boolean byPerm = menu.getPerms() != null
                    && !menu.getPerms().isBlank()
                    && grantedPerms.contains(menu.getPerms());
            if (byParent || byPerm) {
                ids.add(menu.getId());
            }
        }
        return ids;
    }
}
