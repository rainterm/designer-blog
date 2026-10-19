package com.designer.admin.system.spi;

import com.designer.admin.framework.satoken.PermissionCache;
import com.designer.admin.framework.satoken.PermissionLoader;
import com.designer.admin.framework.satoken.PermissionSnapshot;
import com.designer.admin.system.mapper.rbac.SysMenuMapper;
import com.designer.admin.system.mapper.rbac.SysRoleMapper;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import java.util.List;

/**
 * 权限数据源：对接 sys_menu / sys_role，并通过 {@link PermissionCache} 做读缓存。
 *
 * <p><b>缓存放置位置的理由</b>：{@code @SaCheckPermission} 每个请求至少调一次本类，
 * 非超管原本要 2 次 SQL（isSuper + perms）。缓存后命中即 1 次 Redis GET，
 * 回源仍是 2~3 次 SQL，但只在缓存失效或 TTL 到期时发生。</p>
 *
 * <p><b>无权限用户也要缓存空快照</b>：否则一个没有任何角色的账号
 * （或恶意注册的账号）每次请求仍会打 2 次 SQL，缓存等于没生效。</p>
 *
 * @author 雨夜终端
 */
@Component
public class SysPermissionLoader implements PermissionLoader {

    @Db
    private SysMenuMapper menuMapper;

    @Db
    private SysRoleMapper roleMapper;

    @Inject
    private PermissionCache permissionCache;

    @Override
    public PermissionSnapshot load(Long userId) {
        if (userId == null) {
            return PermissionSnapshot.empty();
        }

        PermissionSnapshot cached = permissionCache.get(userId);
        if (cached != null) {
            return cached;
        }

        PermissionSnapshot snapshot = query(userId);
        permissionCache.put(userId, snapshot);
        return snapshot;
    }

    /**
     * 回源装载。
     *
     * <p>超管跳过权限清单查询：它必然拥有全部权限，前端与 Sa-Token 都认通配符 {@code *}，
     * 这时候再去 join 三张表取一堆 perms 是纯浪费。</p>
     */
    private PermissionSnapshot query(Long userId) {
        boolean superAdmin = roleMapper.selectIsSuperByUserId(userId);
        List<String> roles = List.copyOf(roleMapper.selectRoleKeysByUserId(userId));
        List<String> permissions = superAdmin
                ? List.of("*")
                : List.copyOf(menuMapper.selectPermsByUserId(userId));
        return new PermissionSnapshot(superAdmin, permissions, roles);
    }
}
