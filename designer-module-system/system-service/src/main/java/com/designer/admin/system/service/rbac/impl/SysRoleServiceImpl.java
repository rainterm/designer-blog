package com.designer.admin.system.service.rbac.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.common.constant.SystemConsts;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.system.mapper.rbac.SysRoleMapper;
import com.designer.admin.system.mapper.rbac.SysUserRoleMapper;
import com.designer.admin.system.model.dto.rbac.RoleQueryDTO;
import com.designer.admin.system.model.dto.rbac.RoleSaveDTO;
import com.designer.admin.system.model.entity.rbac.SysRole;
import com.designer.admin.system.model.entity.rbac.SysUserRole;
import com.designer.admin.system.service.rbac.SysMenuService;
import com.designer.admin.system.service.rbac.SysRoleService;
import com.designer.admin.system.spi.PermissionInvalidator;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.data.annotation.Transaction;

import java.util.List;
import java.util.Objects;

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
    @Inject
    private SysMenuService menuService;
    @Inject
    private PermissionInvalidator permissionInvalidator;

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
        roleMapper.insert(role);
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
        SysRole update = toEntity(dto);
        // 不允许通过 DTO 篡改超管/类型标记
        update.setIsSuper(existing.getIsSuper());
        update.setRoleType(existing.getRoleType());
        roleMapper.updateById(update);
        if (dto.getMenuIds() != null) {
            menuService.assignRoleMenus(dto.getId(), dto.getMenuIds());
        }
        // 角色标识 / 状态变了才需要整体失效；只改组名、备注时无需动缓存
        boolean identityChanged = !Objects.equals(existing.getRoleKey(), update.getRoleKey())
                || !Objects.equals(existing.getStatus(), update.getStatus());
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
        // 正常路径下没有用户绑定（上面已校验），这里仍删一次：防止历史脏数据残留在缓存里
        permissionInvalidator.evictRoles(ids);
    }

    @Override
    public List<Long> listMenuIds(Long roleId) {
        return menuService.listMenuIdsByRole(roleId);
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
        role.setRemark(dto.getRemark());
        return role;
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
