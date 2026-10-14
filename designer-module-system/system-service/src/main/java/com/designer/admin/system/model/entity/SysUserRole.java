package com.designer.admin.system.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户 - 角色关联表（联合主键：user_id + role_id，无自增主键）
 */
@Data
@TableName("sys_user_role")
public class SysUserRole implements Serializable {

    /** 用户ID */
    @TableField("user_id")
    private Long userId;

    /** 角色ID */
    @TableField("role_id")
    private Long roleId;
}