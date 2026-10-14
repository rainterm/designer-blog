package com.designer.admin.system.model.vo;

import lombok.Data;

/**
 * OAuth2 Client Credentials 令牌响应（RFC 6749）
 */
@Data
public class Oauth2TokenVO {
    private String access_token;
    private String token_type;
    private Long expires_in;
    private String scope;
}
