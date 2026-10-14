package com.designer.admin.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.designer.admin.common.constant.SystemConstants;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.common.util.TreeUtils;
import com.designer.admin.system.model.dto.MenuSaveDTO;
import com.designer.admin.system.model.entity.SysMenu;
import com.designer.admin.system.model.entity.SysRoleMenu;
import com.designer.admin.system.mapper.SysMenuMapper;
import com.designer.admin.system.mapper.SysRoleMenuMapper;
import com.designer.admin.system.model.vo.RouterVO;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;

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
 */
@Component
public class SysMenuService {

    @Db
    private SysMenuMapper menuMapper;
    @Db
    private SysRoleMenuMapper roleMenuMapper;

    /**
     * 构建当前用户可见的动态路由（目录 + 菜单，不含按钮）。
     * 超级管理员取全量启用菜单；普通用户按 RBAC 关联查询后过滤按钮并构建树。
     */
    public List<RouterVO> buildRouters(Long userId, boolean superAdmin) {
        List<SysMenu> menus;
        if (superAdmin) {
            menus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                    .eq(SysMenu::getStatus, SystemConstants.STATUS_ENABLE)
                    .ne(SysMenu::getMenuType, SystemConstants.MENU_TYPE_BUTTON)
                    .orderByAsc(SysMenu::getParentId).orderByAsc(SysMenu::getSort));
        } else {
            menus = menuMapper.selectRouterMenusByUserId(userId).stream()
                    .filter(m -> !SystemConstants.MENU_TYPE_BUTTON.equals(m.getMenuType()))
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
        router.setComponent(SystemConstants.MENU_TYPE_DIR.equals(menu.getMenuType()) ? "Layout" : menu.getComponent());
        RouterVO.MetaVO meta = new RouterVO.MetaVO();
        meta.setTitle(menu.getMenuName());
        meta.setIcon(menu.getIcon());
        meta.setKeepAlive(false);
        // visible=0：详情/编辑等跳转页，注册路由但不进侧栏
        meta.setHidden(menu.getVisible() == null || menu.getVisible() != SystemConstants.STATUS_ENABLE);
        String component = menu.getComponent();
        String path = menu.getPath();
        String linkUrl = resolveLinkUrl(component, path);
        boolean ssoExternal = component != null
                && component.regionMatches(true, 0, "External", 0, "External".length());
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
        if (component != null && component.regionMatches(true, 0, "Link", 0, 4)) {
            int colon = component.indexOf(':');
            if (colon >= 0 && colon < component.length() - 1) {
                String url = component.substring(colon + 1).trim();
                if (!url.isEmpty()) {
                    return url;
                }
            }
        }
        if (path != null && (path.startsWith("http://") || path.startsWith("https://"))) {
            return path;
        }
        return null;
    }

    /** 由路径生成唯一 PascalCase 路由名：/system/user -> SystemUser */
    private String buildRouteName(SysMenu menu) {
        String path = menu.getPath();
        if (path == null || path.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String seg : path.replaceAll("^/+|/+$", "").split("/")) {
            if (seg.isEmpty()) {
                continue;
            }
            sb.append(Character.toUpperCase(seg.charAt(0))).append(seg.substring(1));
        }
        return sb.toString();
    }

    public List<SysMenu> tree(String keyword, Integer status) {
        LambdaQueryWrapper<SysMenu> w = new LambdaQueryWrapper<SysMenu>()
                .like(keyword != null && !keyword.isEmpty(), SysMenu::getMenuName, keyword)
                .eq(status != null, SysMenu::getStatus, status)
                .orderByAsc(SysMenu::getParentId).orderByAsc(SysMenu::getSort);
        return TreeUtils.buildTree(menuMapper.selectList(w), 0L);
    }

    public List<SysMenu> options() {
        return TreeUtils.buildTree(menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .ne(SysMenu::getMenuType, SystemConstants.MENU_TYPE_BUTTON)
                .orderByAsc(SysMenu::getParentId).orderByAsc(SysMenu::getSort)), 0L);
    }

    public SysMenu getById(Long id) {
        return menuMapper.selectById(id);
    }

    /**
     * 新增菜单：父菜单为空时默认为 0（顶级）；新增后若存在父节点，会将父节点修正为目录类型。
     */
    public void save(MenuSaveDTO dto) {
        SysMenu menu = toEntity(dto);
        if (menu.getParentId() == null) {
            menu.setParentId(0L);
        }
        menuMapper.insert(menu);
        adjustParentType(menu);
    }

    public void update(MenuSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("菜单ID不能为空");
        }
        menuMapper.updateById(toEntity(dto));
        adjustParentType(toEntity(dto));
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
            if (parent != null && !SystemConstants.MENU_TYPE_DIR.equals(parent.getMenuType())) {
                SysMenu fix = new SysMenu();
                fix.setId(parent.getId());
                fix.setMenuType(SystemConstants.MENU_TYPE_DIR);
                menuMapper.updateById(fix);
            }
        }
    }

    /**
     * 删除菜单：级联删除全部子孙菜单，并清理 sys_role_menu 中对应关联。
     */
    public void remove(Long id) {
        List<Long> ids = collectDescendantIds(id);
        ids.add(id);
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

    /** 角色已授权的菜单 id 列表 */
    public List<Long> menuIdsByRole(Long roleId) {
        return roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                .eq(SysRoleMenu::getRoleId, roleId))
                .stream()
                .map(SysRoleMenu::getMenuId)
                .toList();
    }

    public void assignRoleMenus(Long roleId, List<Long> menuIds) {
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId));
        Set<Long> ids = expandWithHiddenMenus(menuIds);
        for (Long menuId : ids) {
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(roleId);
            rm.setMenuId(menuId);
            roleMenuMapper.insert(rm);
        }
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
                .eq(SysMenu::getStatus, SystemConstants.STATUS_ENABLE));
        Set<String> grantedPerms = all.stream()
                .filter(m -> ids.contains(m.getId()))
                .map(SysMenu::getPerms)
                .filter(p -> p != null && !p.isBlank())
                .collect(Collectors.toCollection(HashSet::new));
        for (SysMenu menu : all) {
            if (menu.getVisible() != null && menu.getVisible() == SystemConstants.STATUS_ENABLE) {
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