package com.designer.admin.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.designer.admin.system.model.entity.SysUserPost;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户岗位关联数据访问层
 */
@Mapper
public interface SysUserPostMapper extends BaseMapper<SysUserPost> {
}
