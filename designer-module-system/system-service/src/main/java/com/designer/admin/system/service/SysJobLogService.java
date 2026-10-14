package com.designer.admin.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.common.util.AsyncRunner;
import com.designer.admin.framework.job.JobLogRecord;
import com.designer.admin.framework.job.JobLogWriter;
import com.designer.admin.system.mapper.SysJobLogMapper;
import com.designer.admin.system.model.dto.JobLogQueryDTO;
import com.designer.admin.system.model.entity.SysJobLog;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 定时任务调度日志，同时实现 framework {@link JobLogWriter} SPI。
 */
@Component
public class SysJobLogService implements JobLogWriter {

    @Db
    private SysJobLogMapper jobLogMapper;

    @Override
    public void write(JobLogRecord record) {
        if (record == null) {
            return;
        }
        SysJobLog log = new SysJobLog();
        log.setJobId(record.getJobId());
        log.setJobName(record.getJobName());
        log.setJobGroup(record.getJobGroup());
        log.setInvokeTarget(record.getInvokeTarget());
        log.setJobMessage(record.getJobMessage());
        log.setStatus(record.getStatus());
        log.setExceptionInfo(record.getExceptionInfo());
        log.setCostTime(record.getCostTime());
        log.setCreateTime(record.getCreateTime());
        record(log);
    }

    public void record(SysJobLog log) {
        jobLogMapper.insert(log);
    }

    public void recordAsync(SysJobLog log) {
        AsyncRunner.run(() -> {
            try {
                jobLogMapper.insert(log);
            } catch (Exception ignored) {
                // ignore
            }
        });
    }

    public IPage<SysJobLog> page(JobLogQueryDTO query) {
        return jobLogMapper.selectPage(query.toPage(), new LambdaQueryWrapper<SysJobLog>()
                .eq(query.getJobId() != null, SysJobLog::getJobId, query.getJobId())
                .like(notBlank(query.getJobName()), SysJobLog::getJobName, query.getJobName())
                .eq(notBlank(query.getJobGroup()), SysJobLog::getJobGroup, query.getJobGroup())
                .eq(query.getStatus() != null, SysJobLog::getStatus, query.getStatus())
                .orderByDesc(SysJobLog::getId));
    }

    public void removeByIds(String idsCsv) {
        List<Long> ids = parseIds(idsCsv);
        if (ids.isEmpty()) {
            throw new BizException("请选择要删除的日志");
        }
        jobLogMapper.deleteByIds(ids);
    }

    public void clear() {
        jobLogMapper.delete(new LambdaQueryWrapper<SysJobLog>().ge(SysJobLog::getId, 0));
    }

    /** 删除指定天数之前的调度日志，返回删除条数 */
    public int cleanOlderThan(int days) {
        LocalDateTime before = LocalDateTime.now().minusDays(Math.max(days, 1));
        return jobLogMapper.delete(new LambdaQueryWrapper<SysJobLog>()
                .lt(SysJobLog::getCreateTime, before));
    }

    public void clearByJobId(Long jobId) {
        if (jobId == null) {
            throw new BizException("任务ID不能为空");
        }
        jobLogMapper.delete(new LambdaQueryWrapper<SysJobLog>().eq(SysJobLog::getJobId, jobId));
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
}
