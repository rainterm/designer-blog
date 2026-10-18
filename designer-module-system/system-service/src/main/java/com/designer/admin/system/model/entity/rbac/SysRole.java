package com.designer.admin.system.model.entity.rbac;

import com.baomidou.mybatisplus.annotation.TableName;
import com.designer.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统角色（{@code role_key} 全局唯一，作为权限字符串的元组）
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseEntity {

    /** 角色名称（前端展示用） */
    private String roleName;

    /** 角色权限字符（如 {@code admin} / {@code common}，权限比较使用） */
    private String roleKey;

    /** 显示顺序，升序 */
    private Integer sort;

    /** 1正常 0停用 */
    private Integer status;

    /** 是否超级管理员：1是 0否；超管跳过细粒度权限校验（取代原 role_key=admin 字符串判断） */
    private Integer isSuper;

    /** 角色类型：1=系统内置（不可修改/删除） 2=自定义 */
    private Integer roleType;

    /** 备注 */
    private String remark;
}
