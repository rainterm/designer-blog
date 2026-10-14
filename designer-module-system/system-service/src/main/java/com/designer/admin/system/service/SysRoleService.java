package com.designer.admin.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.common.constant.SystemConstants;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.system.model.dto.RoleQueryDTO;
import com.designer.admin.system.model.dto.RoleSaveDTO;
import com.designer.admin.system.model.entity.SysRole;
import com.designer.admin.system.model.entity.SysUserRole;
import com.designer.admin.system.mapper.SysRoleMapper;
import com.designer.admin.system.mapper.SysUserRoleMapper;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.data.annotation.Transaction;

import java.util.List;

/**
 * 角色服务
 */
@Component
public class SysRoleService {

    @Db
    private SysRoleMapper roleMapper;
    @Db
    private SysUserRoleMapper userRoleMapper;
    @Inject
    private SysMenuService menuService;

    public IPage<SysRole> page(RoleQueryDTO query) {
        LambdaQueryWrapper<SysRole> w = new LambdaQueryWrapper<SysRole>()
                .like(notBlank(query.getRoleName()), SysRole::getRoleName, query.getRoleName())
                .like(notBlank(query.getRoleKey()), SysRole::getRoleKey, query.getRoleKey())
                .eq(query.getStatus() != null, SysRole::getStatus, query.getStatus())
                .orderByAsc(SysRole::getSort);
        return roleMapper.selectPage(query.toPage(), w);
    }

    public List<SysRole> options() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getStatus, 1)
                .orderByAsc(SysRole::getSort));
    }

    public SysRole getById(Long id) {
        return roleMapper.selectById(id);
    }

    /**
     * 新增角色：校验 roleKey 唯一；状态/排序为空时填充默认值；可选写入角色-菜单绑定。
     */
    @Transaction
    public void save(RoleSaveDTO dto) {
        Long dup = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleKey, dto.getRoleKey()));
        if (dup != null && dup > 0) {
            throw new BizException("角色标识已存在");
        }
        SysRole role = toEntity(dto);
        if (role.getStatus() == null) {
            role.setStatus(1);
        }
        if (role.getSort() == null) {
            role.setSort(0);
        }
        // 新增角色默认自定义；不允许通过 DTO 创建内置/超管角色
        role.setRoleType(SystemConstants.ROLE_TYPE_CUSTOM);
        role.setIsSuper(SystemConstants.IS_SUPER_NO);
        roleMapper.insert(role);
        if (dto.getMenuIds() != null) {
            menuService.assignRoleMenus(role.getId(), dto.getMenuIds());
        }
    }

    @Transaction
    public void update(RoleSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("角色ID不能为空");
        }
        SysRole existing = roleMapper.selectById(dto.getId());
        if (existing == null) {
            throw new BizException("角色不存在");
        }
        if (Integer.valueOf(SystemConstants.ROLE_TYPE_BUILTIN).equals(existing.getRoleType())) {
            throw new BizException("内置角色不允许修改");
        }
        Long dup = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleKey, dto.getRoleKey())
                .ne(SysRole::getId, dto.getId()));
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
    }

    /**
     * 批量删除角色：内置角色不允许删除；已分配用户的角色不允许删除。
     */
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
            if (Integer.valueOf(SystemConstants.ROLE_TYPE_BUILTIN).equals(role.getRoleType())) {
                throw new BizException("角色【" + role.getRoleName() + "】为内置角色，不允许删除");
            }
            Long bound = userRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRole>()
                    .eq(SysUserRole::getRoleId, id));
            if (bound != null && bound > 0) {
                throw new BizException("角色【" + role.getRoleName() + "】已分配用户，无法删除");
            }
        }
        roleMapper.deleteByIds(ids);
    }

    public List<Long> menuIds(Long roleId) {
        return menuService.menuIdsByRole(roleId);
    }

    /**
     * 角色授权菜单。超级管理员角色默认拥有全部权限，不允许也不需要写入 sys_role_menu。
     */
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
        if (Integer.valueOf(SystemConstants.IS_SUPER_YES).equals(role.getIsSuper())) {
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
