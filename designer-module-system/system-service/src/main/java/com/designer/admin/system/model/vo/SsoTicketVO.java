package com.designer.admin.system.model.vo;

import lombok.Data;

/**
 * 一次性 SSO 票据
 */
@Data
public class SsoTicketVO {

    private String ticket;

    /** 有效期（秒） */
    private long expiresIn;
}
