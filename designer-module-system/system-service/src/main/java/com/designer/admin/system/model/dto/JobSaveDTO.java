package com.designer.admin.system.model.dto;

import lombok.Data;

@Data
public class JobSaveDTO {

    private Long id;
    private String jobName;
    private String jobGroup;
    private String invokeTarget;
    private String cronExpression;
    private Integer misfirePolicy;
    private Integer concurrent;
    private Integer status;
    private String remark;
}
