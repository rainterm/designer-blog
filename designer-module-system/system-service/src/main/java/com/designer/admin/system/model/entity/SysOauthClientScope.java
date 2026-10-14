package com.designer.admin.system.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * OAuth 客户端与 Scope 关联
 */
@Data
@TableName("sys_oauth_client_scope")
public class SysOauthClientScope {
    private Long clientId;
    private Long scopeId;
}
