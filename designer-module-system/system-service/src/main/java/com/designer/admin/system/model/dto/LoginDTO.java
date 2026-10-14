package com.designer.admin.system.model.dto;

import com.designer.admin.common.annotation.Sensitive;
import lombok.Data;
import org.noear.solon.validation.annotation.NotBlank;

/**
 * 登录入参
 */
@Data
public class LoginDTO {

    @NotBlank(message = "账号不能为空")
    private String username;

    @Sensitive
    @NotBlank(message = "密码不能为空")
    private String password;

    /** 验证码（开启验证码时必填） */
    private String captcha;

    /** 验证码唯一标识 */
    private String uuid;
}
