package com.designer.admin.system.model.dto;

import lombok.Data;
import org.noear.solon.validation.annotation.NotBlank;

/**
 * SSO 票据换 token
 */
@Data
public class SsoExchangeDTO {

    @NotBlank(message = "票据不能为空")
    private String ticket;
}
