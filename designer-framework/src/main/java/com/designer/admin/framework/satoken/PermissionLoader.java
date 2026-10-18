package com.designer.admin.framework.satoken;

import java.util.List;

/**
 * 权限数据源 SPI：由 system 模块实现，framework 不直接依赖 Sys* Mapper。
 *
 * @author 雨夜终端
 */
public interface PermissionLoader {

    /**
     * 判断是否超级管理员（拥有全部权限）。
     *
     * @param userId 用户 ID
     * @return 超级管理员返回 {@code true}
     */
    boolean isSuper(Long userId);

    /**
     * 查询用户权限标识列表。
     *
     * @param userId 用户 ID
     * @return 权限标识列表，如 {@code system:user:list}
     */
    List<String> getPermissionList(Long userId);

    /**
     * 查询用户角色标识列表。
     *
     * @param userId 用户 ID
     * @return 角色标识列表，如 {@code admin}
     */
    List<String> getRoleList(Long userId);
}
