package com.designer.admin.system.mapper;

import com.github.yulichang.base.MPJBaseMapper;
import com.github.yulichang.toolkit.JoinWrappers;
import com.designer.admin.common.constant.SystemConstants;
import com.designer.admin.system.model.entity.SysRole;
import com.designer.admin.system.model.entity.SysUserRole;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 系统角色数据访问层。关联查询（join）改用 mpj Wrapper 在 service 层调用，不再写 SQL 注解。
 */
@Mapper
public interface SysRoleMapper extends MPJBaseMapper<SysRole> {

    /**
     * 查询用户已启用的角色权限字符（{@code role_key}，去重）。
     */
    default List<String> selectRoleKeysByUserId(Long userId) {
        return selectJoinList(String.class, JoinWrappers.lambda(SysRole.class)
                .select(SysRole::getRoleKey)
                .distinct()
                .leftJoin(SysUserRole.class, SysUserRole::getRoleId, SysRole::getId)
                .eq(SysUserRole::getUserId, userId)
                .eq(SysRole::getStatus, SystemConstants.STATUS_ENABLE));
    }

    /**
     * 查询用户的全部角色 ID（含停用，用于角色分配回显）。
     */
    default List<Long> selectRoleIdsByUserId(Long userId) {
        return selectJoinList(Long.class, JoinWrappers.lambda(SysRole.class)
                .select(SysRole::getId)
                .leftJoin(SysUserRole.class, SysUserRole::getRoleId, SysRole::getId)
                .eq(SysUserRole::getUserId, userId));
    }

    /**
     * 判断用户是否为超级管理员：任一启用的角色 is_super=1 即视为超管。
     * 用 COUNT(*) 短路（命中即停）替代 EXISTS，避免方言差异。
     */
    default boolean selectIsSuperByUserId(Long userId) {
        Long cnt = selectJoinCount(JoinWrappers.lambda(SysRole.class)
                .leftJoin(SysUserRole.class, SysUserRole::getRoleId, SysRole::getId)
                .eq(SysUserRole::getUserId, userId)
                .eq(SysRole::getIsSuper, SystemConstants.IS_SUPER_YES)
                .eq(SysRole::getStatus, SystemConstants.STATUS_ENABLE));
        return cnt != null && cnt > 0;
    }
}