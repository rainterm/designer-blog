package com.designer.admin.system.model.dto;

import lombok.Data;

import java.util.List;

/**
 * 角色保存（新增/修改通用）
 */
@Data
public class RoleSaveDTO {
    private Long id;
    private String roleName;
    private String roleKey;
    private Integer sort;
    private Integer status;
    /** 角色类型：1=内置 2=自定义（仅 service 在创建时强制设为 CUSTOM，DTO 字段保留用于回显） */
    private Integer roleType;
    private String remark;
    /** 授权的菜单ID列表，为空表示不改动 */
    private List<Long> menuIds;
}
