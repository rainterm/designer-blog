package com.designer.admin.system.model.dto;

import lombok.Data;

/**
 * 签发 SSO 票据；可选带上 redirect，由服务端校验白名单
 */
@Data
public class SsoTicketDTO {
    /** 客户端回跳完整 URL（含 path/query），用于服务端 Origin 校验 */
    private String redirect;
}
