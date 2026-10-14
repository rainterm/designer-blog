package com.designer.admin.system.model.vo;

import lombok.Data;

import java.util.List;

/**
 * 当前登录用户信息（含角色与权限标识）
 */
@Data
public class UserInfoVO {

    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private String email;
    private String phone;

    /** 角色标识列表 */
    private List<String> roles;

    /** 权限标识列表；超级管理员为 ["*"] */
    private List<String> perms;

    /** 是否超级管理员 */
    private boolean superAdmin;
}
