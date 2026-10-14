package com.designer.admin.framework.job;

import org.quartz.JobExecutionContext;

/**
 * 允许并发执行的 Quartz Job
 */
public class QuartzJobExecution extends AbstractQuartzJob {

    @Override
    protected void doExecute(JobExecutionContext context, JobModel job) throws Exception {
        JobInvokeUtils.invokeMethod(job.getInvokeTarget());
    }
}
