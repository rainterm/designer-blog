package com.designer.admin.system.mapper.rbac;

import com.designer.admin.system.model.entity.rbac.SysUser;
import com.github.yulichang.base.MPJBaseMapper;
import com.github.yulichang.toolkit.JoinWrappers;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统用户数据访问层
 *
 * @author 雨夜终端
 */
@Mapper
public interface SysUserMapper extends MPJBaseMapper<SysUser> {

    /**
     * 统计登录账号占用数（只统计未删除的用户）。
     *
     * <p>账号唯一性完全在应用层判断：表上没有唯一索引，所以逻辑删除的用户不再占着账号，
     * 删除后可以用同一个账号重新建号。这里的查询必须走 {@code @TableLogic} 默认条件
     * （即只看 {@code deleted = 0}），否则已删除的账号会永远无法复用。</p>
     *
     * @param username  登录账号
     * @param excludeId 需要排除的用户 ID，新增时传 {@code null}
     * @return 占用该账号的用户数
     */
    default Long countByUsername(String username, Long excludeId) {
        return selectJoinCount(JoinWrappers.lambda(SysUser.class)
                .eq(SysUser::getUsername, username)
                .ne(excludeId != null, SysUser::getId, excludeId));
    }
}
