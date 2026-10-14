package com.designer.admin.system.model.dto;

import com.designer.admin.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OauthClientQueryDTO extends PageQuery {
    private String clientId;
    private String clientName;
    private Integer status;
}
