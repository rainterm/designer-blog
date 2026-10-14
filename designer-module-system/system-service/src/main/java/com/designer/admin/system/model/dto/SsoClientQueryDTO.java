package com.designer.admin.system.model.dto;

import com.designer.admin.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * SSO 客户端查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SsoClientQueryDTO extends PageQuery {
    private String clientName;
    private String clientKey;
    private Integer status;
}
