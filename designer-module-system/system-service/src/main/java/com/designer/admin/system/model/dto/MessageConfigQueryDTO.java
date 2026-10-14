package com.designer.admin.system.model.dto;

import com.designer.admin.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MessageConfigQueryDTO extends PageQuery {

    private String configName;
    private String channelType;
    private Integer status;
}
