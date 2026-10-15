package com.designer.admin.system.model.dto;

import com.designer.admin.common.annotation.Sensitive;
import com.designer.admin.common.constant.PasswordPolicy;
import lombok.Data;
import org.noear.solon.validation.annotation.Length;
import org.noear.solon.validation.annotation.NotBlank;

/**
 * 登录入参
 */
@Data
public class LoginDTO {

    /**
     * 账号最大长度：与 {@code sys_user.username VARCHAR(50)} 对齐。
     *
     * <p>前端登录页用的是同一个值（见 admin-web 的 login/index.vue），改动时两边要一起改。</p>
     */
    public static final int USERNAME_MAX_LENGTH = 50;

    @NotBlank(message = "账号不能为空")
    @Length(max = USERNAME_MAX_LENGTH, message = "账号长度不能超过 " + USERNAME_MAX_LENGTH + " 个字符")
    private String username;

    /**
     * 密码：登录侧**只卡长度、不卡复杂度**，与写入侧（新增 / 重置 / 改密）不同。
     *
     * <p>卡长度是为了让超短密码在消耗一次 BCrypt 比对（约百毫秒，也是登录接口最贵的一步）
     * 之前就被拒掉。不卡复杂度是因为复杂度是对「新设密码」的要求：
     * 一旦在登录侧也校验，历史上那些纯字母或纯数字的老密码即使输对了也登不进来，
     * 账号会被直接锁死。登录只回答「输入形态是否合理」。</p>
     *
     * <p>能这么卡的前提是写入侧同样卡了下限 —— 否则历史遗留的短密码账号会在这里被误拒，
     * 见 {@code SysUserService} 的兜底校验。</p>
     */
    @Sensitive
    @NotBlank(message = "密码不能为空")
    @Length(min = PasswordPolicy.MIN_LENGTH, max = PasswordPolicy.MAX_LENGTH,
            message = PasswordPolicy.LENGTH_MESSAGE)
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
