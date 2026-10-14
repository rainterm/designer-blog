package com.designer.admin.system.model.dto;

import com.designer.admin.common.annotation.Sensitive;
import lombok.Data;
import org.noear.solon.validation.annotation.NotBlank;

/**
 * 重置密码
 */
@Data
public class ResetPwdDTO {

    private Long userId;

    @Sensitive
    @NotBlank(message = "新密码不能为空")
    private String password;
}
