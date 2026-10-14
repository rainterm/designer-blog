package com.designer.admin.framework.job;

import com.designer.admin.common.util.AsyncRunner;
import org.noear.solon.Solon;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

/**
 * Quartz 抽象任务：执行 invoke_target 并写调度日志
 */
public abstract class AbstractQuartzJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(AbstractQuartzJob.class);

    @Override
    public void execute(JobExecutionContext context) {
        JobModel job = (JobModel) context.getMergedJobDataMap().get(ScheduleConstants.TASK_PROPERTIES);
        long start = System.currentTimeMillis();
        try {
            before(context, job);
            if (job != null) {
                doExecute(context, job);
            }
            after(context, job, null, System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("任务执行异常 jobId={}", job == null ? null : job.getId(), e);
            after(context, job, e, System.currentTimeMillis() - start);
        }
    }

    protected void before(JobExecutionContext context, JobModel job) {
        // hook
    }

    protected abstract void doExecute(JobExecutionContext context, JobModel job) throws Exception;

    protected void after(JobExecutionContext context, JobModel job, Exception e, long cost) {
        if (job == null) {
            return;
        }
        JobLogRecord jobLog = new JobLogRecord();
        jobLog.setJobId(job.getId());
        jobLog.setJobName(job.getJobName());
        jobLog.setJobGroup(job.getJobGroup());
        jobLog.setInvokeTarget(job.getInvokeTarget());
        jobLog.setCostTime(cost);
        jobLog.setCreateTime(LocalDateTime.now());
        if (e != null) {
            jobLog.setStatus(0);
            String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            jobLog.setJobMessage(job.getJobName() + " 执行失败，耗时 " + cost + " ms");
            jobLog.setExceptionInfo(msg.length() > 2000 ? msg.substring(0, 2000) : msg);
        } else {
            jobLog.setStatus(1);
            jobLog.setJobMessage(job.getJobName() + " 执行成功，耗时 " + cost + " ms");
            jobLog.setExceptionInfo("");
        }
        AsyncRunner.run(() -> {
            JobLogWriter writer = Solon.context().getBean(JobLogWriter.class);
            if (writer != null) {
                writer.write(jobLog);
            }
        });
    }
}
