package com.designer.admin.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.designer.admin.system.model.entity.SysUserRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户角色关联数据访问层
 */
@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {
}
