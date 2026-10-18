package com.designer.admin.framework.job;

import com.designer.admin.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Destroy;
import org.noear.solon.annotation.Init;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;

/**
 * Quartz 调度器封装：创建 / 更新 / 删除 / 暂停 / 恢复 / 立即执行
 *
 * @author 雨夜终端
 */
@Component
@Slf4j
public class ScheduleManager {

    private Scheduler scheduler;

    @Init(index = 1)
    public void start() throws SchedulerException {
        scheduler = StdSchedulerFactory.getDefaultScheduler();
        scheduler.start();
        log.info("Quartz Scheduler started");
    }

    @Destroy
    public void stop() {
        if (scheduler != null) {
            try {
                scheduler.shutdown(true);
                log.info("Quartz Scheduler stopped");
            } catch (SchedulerException e) {
                log.warn("Quartz Scheduler shutdown error", e);
            }
        }
    }

    public void createJob(JobModel job) {
        try {
            JobKey jobKey = jobKey(job);
            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
            }
            Class<? extends org.quartz.Job> jobClass = resolveJobClass(job);
            JobDetail jobDetail = JobBuilder.newJob(jobClass)
                    .withIdentity(jobKey)
                    .build();
            jobDetail.getJobDataMap().put(ScheduleConsts.TASK_PROPERTIES, job);

            CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(job.getCronExpression());
            scheduleBuilder = handleMisfire(scheduleBuilder, job.getMisfirePolicy());

            CronTrigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey(job))
                    .withSchedule(scheduleBuilder)
                    .build();

            scheduler.scheduleJob(jobDetail, trigger);
            if (job.getStatus() != null && job.getStatus() == ScheduleConsts.STATUS_PAUSE) {
                pauseJob(job);
            }
        } catch (SchedulerException e) {
            throw new BizException("创建定时任务失败：" + e.getMessage());
        }
    }

    public void updateJob(JobModel job) {
        try {
            JobKey jobKey = jobKey(job);
            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
            }
            createJob(job);
        } catch (SchedulerException e) {
            throw new BizException("更新定时任务失败：" + e.getMessage());
        }
    }

    public void deleteJob(JobModel job) {
        try {
            scheduler.deleteJob(jobKey(job));
        } catch (SchedulerException e) {
            throw new BizException("删除定时任务失败：" + e.getMessage());
        }
    }

    public void pauseJob(JobModel job) {
        try {
            scheduler.pauseJob(jobKey(job));
        } catch (SchedulerException e) {
            throw new BizException("暂停定时任务失败：" + e.getMessage());
        }
    }

    public void resumeJob(JobModel job) {
        try {
            scheduler.resumeJob(jobKey(job));
        } catch (SchedulerException e) {
            throw new BizException("恢复定时任务失败：" + e.getMessage());
        }
    }

    public void runJobOnce(JobModel job) {
        try {
            JobDataMap dataMap = new JobDataMap();
            dataMap.put(ScheduleConsts.TASK_PROPERTIES, job);
            JobKey key = jobKey(job);
            if (!scheduler.checkExists(key)) {
                createJob(job);
            }
            scheduler.triggerJob(key, dataMap);
        } catch (SchedulerException e) {
            throw new BizException("立即执行失败：" + e.getMessage());
        }
    }

    private CronScheduleBuilder handleMisfire(CronScheduleBuilder builder, Integer policy) {
        if (policy == null) {
            return builder;
        }
        return switch (policy) {
            case ScheduleConsts.MISFIRE_IGNORE_MISFIRES -> builder.withMisfireHandlingInstructionIgnoreMisfires();
            case ScheduleConsts.MISFIRE_DO_NOTHING -> builder.withMisfireHandlingInstructionDoNothing();
            default -> builder.withMisfireHandlingInstructionFireAndProceed();
        };
    }

    private Class<? extends org.quartz.Job> resolveJobClass(JobModel job) {
        if (job.getConcurrent() != null && job.getConcurrent() == ScheduleConsts.CONCURRENT_DISALLOW) {
            return QuartzDisallowConcurrentExecution.class;
        }
        return QuartzJobExecution.class;
    }

    public static JobKey jobKey(JobModel job) {
        return JobKey.jobKey(ScheduleConsts.TASK_CLASS_NAME + job.getId(), job.getJobGroup());
    }

    public static TriggerKey triggerKey(JobModel job) {
        return TriggerKey.triggerKey(ScheduleConsts.TASK_CLASS_NAME + job.getId(), job.getJobGroup());
    }
}
