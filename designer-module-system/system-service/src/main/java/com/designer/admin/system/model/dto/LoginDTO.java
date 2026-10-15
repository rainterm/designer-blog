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

    /**
     * 滑块验证码的二次校验凭据。
     *
     * <p>前端拖拽通过后调用 {@code POST /captcha/check} 得到（见
     * {@code com.designer.admin.framework.captcha.CaptchaVerifier}）。**一次性**：
     * 校验通过后服务端就把它作废，同一个值不能重复提交。
     * 仅在 {@code /auth/captcha} 返回 {@code enabled=true} 时必填。</p>
     */
    private String captchaVerification;
}
