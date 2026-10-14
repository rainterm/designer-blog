package com.designer.admin.framework.satoken;

import java.util.List;

/**
 * 权限数据源 SPI：由 system 模块实现，framework 不直接依赖 Sys* Mapper。
 */
public interface PermissionLoader {

    /** 是否超级管理员（拥有全部权限） */
    boolean isSuper(Long userId);

    /** 用户权限标识列表 */
    List<String> getPermissionList(Long userId);

    /** 用户角色标识列表 */
    List<String> getRoleList(Long userId);
}
