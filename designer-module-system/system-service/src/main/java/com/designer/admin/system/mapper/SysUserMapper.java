package com.designer.admin.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.designer.admin.system.model.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 系统用户数据访问层
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 统计登录账号占用数（含逻辑删除，避免唯一索引冲突时无友好提示）。
     */
    @Select("""
            SELECT COUNT(1) FROM sys_user
            WHERE username = #{username}
              AND (#{excludeId} IS NULL OR id <> #{excludeId})
            """)
    Long countByUsername(@Param("username") String username, @Param("excludeId") Long excludeId);
}
