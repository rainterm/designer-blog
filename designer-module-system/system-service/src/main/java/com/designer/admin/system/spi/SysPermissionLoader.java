package com.designer.admin.system.spi;

import com.designer.admin.framework.satoken.PermissionLoader;
import com.designer.admin.system.mapper.rbac.SysMenuMapper;
import com.designer.admin.system.mapper.rbac.SysRoleMapper;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;

import java.util.List;

/**
 * 权限数据源：对接 sys_menu / sys_role。
 *
 * @author 雨夜终端
 */
@Component
public class SysPermissionLoader implements PermissionLoader {

    @Db
    private SysMenuMapper menuMapper;

    @Db
    private SysRoleMapper roleMapper;

    @Override
    public boolean isSuper(Long userId) {
        return roleMapper.selectIsSuperByUserId(userId);
    }

    @Override
    public List<String> getPermissionList(Long userId) {
        return menuMapper.selectPermsByUserId(userId);
    }

    @Override
    public List<String> getRoleList(Long userId) {
        return roleMapper.selectRoleKeysByUserId(userId);
    }
}
