package com.designer.admin.framework.job;

import lombok.Data;

import java.io.Serializable;

/**
 * 定时任务运行时模型（framework 侧，与 sys_job 调度字段对齐）。
 *
 * @author 雨夜终端
 */
@Data
public class JobModel implements Serializable {

    private Long id;
    private String jobName;
    private String jobGroup;
    private String invokeTarget;
    private String cronExpression;
    private Integer misfirePolicy;
    private Integer concurrent;
    private Integer status;
}
