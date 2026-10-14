package com.designer.admin.framework.satoken;

import cn.dev33.satoken.stp.StpInterface;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import java.util.Collections;
import java.util.List;

/**
 * Sa-Token 权限数据源：为 @SaCheckPermission / @SaCheckRole 提供权限与角色清单。
 * 超级管理员直接返回通配符 * ，跳过细粒度校验。
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    private static final String WILDCARD = "*";

    @Inject
    private PermissionLoader permissionLoader;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Long userId = toUserId(loginId);
        if (userId == null) {
            return Collections.emptyList();
        }
        if (permissionLoader.isSuper(userId)) {
            return List.of(WILDCARD);
        }
        return permissionLoader.getPermissionList(userId);
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long userId = toUserId(loginId);
        return userId == null ? Collections.emptyList() : permissionLoader.getRoleList(userId);
    }

    private Long toUserId(Object loginId) {
        if (loginId == null) {
            return null;
        }
        try {
            return Long.valueOf(loginId.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
