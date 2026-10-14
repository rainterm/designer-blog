package com.designer.admin.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.framework.job.CronUtils;
import com.designer.admin.framework.job.JobInvokeUtils;
import com.designer.admin.framework.job.JobModel;
import com.designer.admin.framework.job.ScheduleConstants;
import com.designer.admin.framework.job.ScheduleManager;
import com.designer.admin.system.mapper.SysJobMapper;
import com.designer.admin.system.model.dto.JobQueryDTO;
import com.designer.admin.system.model.dto.JobSaveDTO;
import com.designer.admin.system.model.entity.SysJob;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Init;
import org.noear.solon.annotation.Inject;
import org.noear.solon.data.annotation.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 定时任务：CRUD + 启停/立即执行，并与 Quartz 同步
 */
@Component
public class SysJobService {

    private static final Logger log = LoggerFactory.getLogger(SysJobService.class);

    @Db
    private SysJobMapper jobMapper;

    @Inject
    private ScheduleManager scheduleManager;

    /** 启动后将库中任务注册到 Quartz（依赖 ScheduleManager 已启动） */
    @Init(index = 20)
    public void loadJobs() {
        List<SysJob> jobs = jobMapper.selectList(new LambdaQueryWrapper<SysJob>().orderByAsc(SysJob::getId));
        for (SysJob job : jobs) {
            try {
                scheduleManager.createJob(toJobModel(job));
            } catch (Exception e) {
                log.error("加载定时任务失败 id={} name={}", job.getId(), job.getJobName(), e);
            }
        }
        log.info("已加载定时任务 {} 条", jobs.size());
    }

    public IPage<SysJob> page(JobQueryDTO query) {
        return jobMapper.selectPage(query.toPage(), new LambdaQueryWrapper<SysJob>()
                .like(notBlank(query.getJobName()), SysJob::getJobName, query.getJobName())
                .eq(notBlank(query.getJobGroup()), SysJob::getJobGroup, query.getJobGroup())
                .eq(query.getStatus() != null, SysJob::getStatus, query.getStatus())
                .orderByDesc(SysJob::getId));
    }

    public SysJob getById(Long id) {
        SysJob job = jobMapper.selectById(id);
        if (job == null) {
            throw new BizException("任务不存在");
        }
        return job;
    }

    public Map<String, Object> detailView(Long id) {
        SysJob job = getById(id);
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", job.getId());
        view.put("jobName", job.getJobName());
        view.put("jobGroup", job.getJobGroup());
        view.put("invokeTarget", job.getInvokeTarget());
        view.put("cronExpression", job.getCronExpression());
        view.put("misfirePolicy", job.getMisfirePolicy());
        view.put("concurrent", job.getConcurrent());
        view.put("status", job.getStatus());
        view.put("remark", job.getRemark());
        view.put("createTime", job.getCreateTime());
        view.put("updateTime", job.getUpdateTime());
        Date next = CronUtils.nextTriggerTime(job.getCronExpression());
        view.put("nextValidTime", next == null ? null : next.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
        return view;
    }

    @Transaction
    public void save(JobSaveDTO dto) {
        SysJob job = toEntity(dto, true);
        validateJob(job);
        jobMapper.insert(job);
        scheduleManager.createJob(toJobModel(job));
    }

    @Transaction
    public void update(JobSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("任务ID不能为空");
        }
        SysJob exist = getById(dto.getId());
        SysJob job = toEntity(dto, false);
        job.setId(exist.getId());
        validateJob(job);
        jobMapper.updateById(job);
        scheduleManager.updateJob(toJobModel(job));
    }

    @Transaction
    public void removeByIds(String idsCsv) {
        List<Long> ids = parseIds(idsCsv);
        if (ids.isEmpty()) {
            throw new BizException("请选择要删除的任务");
        }
        for (Long id : ids) {
            SysJob job = jobMapper.selectById(id);
            if (job != null) {
                scheduleManager.deleteJob(toJobModel(job));
                jobMapper.deleteById(id);
            }
        }
    }

    @Transaction
    public void changeStatus(Long id, Integer status) {
        if (status == null || (status != ScheduleConstants.STATUS_NORMAL && status != ScheduleConstants.STATUS_PAUSE)) {
            throw new BizException("状态无效");
        }
        SysJob job = getById(id);
        job.setStatus(status);
        jobMapper.updateById(job);
        if (status == ScheduleConstants.STATUS_NORMAL) {
            scheduleManager.resumeJob(toJobModel(job));
        } else {
            scheduleManager.pauseJob(toJobModel(job));
        }
    }

    public void runOnce(Long id) {
        SysJob job = getById(id);
        scheduleManager.runJobOnce(toJobModel(job));
    }

    private JobModel toJobModel(SysJob job) {
        JobModel model = new JobModel();
        model.setId(job.getId());
        model.setJobName(job.getJobName());
        model.setJobGroup(job.getJobGroup());
        model.setInvokeTarget(job.getInvokeTarget());
        model.setCronExpression(job.getCronExpression());
        model.setMisfirePolicy(job.getMisfirePolicy());
        model.setConcurrent(job.getConcurrent());
        model.setStatus(job.getStatus());
        return model;
    }

    private void validateJob(SysJob job) {
        if (!notBlank(job.getJobName())) {
            throw new BizException("任务名称不能为空");
        }
        if (!notBlank(job.getInvokeTarget())) {
            throw new BizException("调用目标不能为空");
        }
        if (!notBlank(job.getCronExpression()) || !CronUtils.isValid(job.getCronExpression())) {
            throw new BizException("Cron 表达式不正确");
        }
        JobInvokeUtils.validate(job.getInvokeTarget());
    }

    private SysJob toEntity(JobSaveDTO dto, boolean creating) {
        SysJob job = new SysJob();
        job.setId(dto.getId());
        job.setJobName(trim(dto.getJobName()));
        job.setJobGroup(notBlank(dto.getJobGroup()) ? dto.getJobGroup().trim() : "DEFAULT");
        job.setInvokeTarget(trim(dto.getInvokeTarget()));
        job.setCronExpression(trim(dto.getCronExpression()));
        job.setMisfirePolicy(dto.getMisfirePolicy() == null ? ScheduleConstants.MISFIRE_DEFAULT : dto.getMisfirePolicy());
        job.setConcurrent(dto.getConcurrent() == null ? ScheduleConstants.CONCURRENT_ALLOW : dto.getConcurrent());
        job.setStatus(dto.getStatus() == null
                ? (creating ? ScheduleConstants.STATUS_PAUSE : ScheduleConstants.STATUS_NORMAL)
                : dto.getStatus());
        job.setRemark(dto.getRemark() == null ? "" : dto.getRemark());
        return job;
    }

    private static List<Long> parseIds(String idsCsv) {
        if (idsCsv == null || idsCsv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(idsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .toList();
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
