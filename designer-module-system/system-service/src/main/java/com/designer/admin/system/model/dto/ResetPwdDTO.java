package com.designer.admin.system.model.dto;

import com.designer.admin.common.annotation.Sensitive;
import com.designer.admin.common.constant.PasswordPolicy;
import lombok.Data;
import org.noear.solon.validation.annotation.Length;
import org.noear.solon.validation.annotation.NotBlank;
import org.noear.solon.validation.annotation.Pattern;

/**
 * 重置密码
 */
@Data
public class ResetPwdDTO {

    private Long userId;

    /**
     * 新密码：必填，长度与复杂度按 {@link PasswordPolicy} 卡（与新增用户、个人改密同一份规则）。
     *
     * <p>「不能与当前密码相同」不在这里判：那需要读库拿到哈希，
     * 由 {@code SysUserService#resetPassword} 负责。</p>
     */
    @Sensitive
    @NotBlank(message = "新密码不能为空")
    @Length(min = PasswordPolicy.MIN_LENGTH, max = PasswordPolicy.MAX_LENGTH,
            message = PasswordPolicy.LENGTH_MESSAGE)
    @Pattern(value = PasswordPolicy.STRENGTH_REGEXP, message = PasswordPolicy.COMPLEXITY_MESSAGE)
    private String password;
}
