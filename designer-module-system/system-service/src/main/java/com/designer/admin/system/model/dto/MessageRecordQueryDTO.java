package com.designer.admin.system.model.dto;

import com.designer.admin.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MessageRecordQueryDTO extends PageQuery {

    private String channelType;
    private String title;
    private String receivers;
    private String sendBy;
    private Integer status;
}
