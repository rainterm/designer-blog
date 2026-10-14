package com.designer.admin.system.model.vo;

import lombok.Data;

/**
 * 创建/重置密钥后仅返回一次明文 secret
 */
@Data
public class OauthClientCreatedVO {
    private Long id;
    private String clientId;
    private String clientSecret;
    private String clientName;
}
