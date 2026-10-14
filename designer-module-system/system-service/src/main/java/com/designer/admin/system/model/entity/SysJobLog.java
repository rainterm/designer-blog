package com.designer.admin.system.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.noear.snack4.annotation.ONodeAttr;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 定时任务调度日志
 */
@Data
@TableName("sys_job_log")
public class SysJobLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long jobId;

    private String jobName;

    private String jobGroup;

    private String invokeTarget;

    private String jobMessage;

    /** 1成功 0失败 */
    private Integer status;

    private String exceptionInfo;

    private Long costTime;

    @ONodeAttr(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
