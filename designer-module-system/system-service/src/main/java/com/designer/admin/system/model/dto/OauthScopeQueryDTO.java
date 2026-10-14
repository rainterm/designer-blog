package com.designer.admin.system.model.dto;

import com.designer.admin.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OauthScopeQueryDTO extends PageQuery {
    private String scopeCode;
    private String scopeName;
    private Integer status;
}
