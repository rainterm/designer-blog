package com.designer.admin.system.model.vo;

import lombok.Data;

import java.util.List;

/**
 * 登录结果
 */
@Data
public class LoginVO {

    /** token 请求头名称 */
    private String tokenName;

    /** token 值 */
    private String token;

    /** 有效期（秒） */
    private long expiresIn;

    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private List<String> roles;
}
