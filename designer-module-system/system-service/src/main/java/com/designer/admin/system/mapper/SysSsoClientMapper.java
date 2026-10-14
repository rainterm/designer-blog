package com.designer.admin.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.designer.admin.system.model.entity.SysSsoClient;
import org.apache.ibatis.annotations.Mapper;

/**
 * SSO 客户端数据访问层
 */
@Mapper
public interface SysSsoClientMapper extends BaseMapper<SysSsoClient> {
}
