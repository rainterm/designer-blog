package com.designer.admin.system.model.dto;

import com.designer.admin.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class JobLogQueryDTO extends PageQuery {

    private Long jobId;
    private String jobName;
    private String jobGroup;
    private Integer status;
}
