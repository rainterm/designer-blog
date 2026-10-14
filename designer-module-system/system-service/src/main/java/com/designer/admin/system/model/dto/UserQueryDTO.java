package com.designer.admin.system.model.dto;

import com.designer.admin.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserQueryDTO extends PageQuery {

    private String username;
    private String nickname;
    private String phone;
    private Integer status;
    /** 按已分配角色筛选 */
    private Long roleId;
    /** 按部门筛选（含下级时由服务端展开） */
    private Long deptId;
    /** 创建时间范围，格式 yyyy-MM-dd */
    private String beginTime;
    private String endTime;
}
