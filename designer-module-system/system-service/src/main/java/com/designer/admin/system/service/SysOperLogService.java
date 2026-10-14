package com.designer.admin.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.common.core.PageQuery;
import com.designer.admin.common.util.AsyncRunner;
import com.designer.admin.framework.log.OperLogRecord;
import com.designer.admin.framework.log.OperLogWriter;
import com.designer.admin.system.model.entity.SysOperLog;
import com.designer.admin.system.mapper.SysOperLogMapper;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;

import java.time.LocalDateTime;

/**
 * 操作日志服务，同时实现 framework {@link OperLogWriter} SPI。
 */
@Component
public class SysOperLogService implements OperLogWriter {

    @Db
    private SysOperLogMapper operLogMapper;

    @Override
    public void writeAsync(OperLogRecord record) {
        if (record == null) {
            return;
        }
        SysOperLog log = new SysOperLog();
        log.setTitle(record.getTitle());
        log.setBusinessType(record.getBusinessType());
        log.setMethod(record.getMethod());
        log.setRequestMethod(record.getRequestMethod());
        log.setOperName(record.getOperName());
        log.setOperUrl(record.getOperUrl());
        log.setOperIp(record.getOperIp());
        log.setOperParam(record.getOperParam());
        log.setJsonResult(record.getJsonResult());
        log.setStatus(record.getStatus());
        log.setErrorMsg(record.getErrorMsg());
        log.setCostTime(record.getCostTime());
        log.setOperTime(record.getOperTime());
        recordAsync(log);
    }

    /** 异步落库，不阻塞主请求（敏感字段依赖 {@code @Sensitive} 序列化脱敏） */
    public void recordAsync(SysOperLog log) {
        AsyncRunner.run(() -> {
            try {
                operLogMapper.insert(log);
            } catch (Exception ignored) {
                // 忽略日志写入异常
            }
        });
    }

    public IPage<SysOperLog> page(PageQuery query, String title, String operName, Integer status) {
        return operLogMapper.selectPage(query.toPage(), buildWrapper(title, operName, status));
    }

    private LambdaQueryWrapper<SysOperLog> buildWrapper(String title, String operName, Integer status) {
        return new LambdaQueryWrapper<SysOperLog>()
                .like(title != null && !title.isEmpty(), SysOperLog::getTitle, title)
                .like(operName != null && !operName.isEmpty(), SysOperLog::getOperName, operName)
                .eq(status != null, SysOperLog::getStatus, status)
                .orderByDesc(SysOperLog::getId);
    }

    /** 删除指定天数之前的操作日志，返回删除条数（供定时任务调用） */
    public int cleanOlderThan(int days) {
        LocalDateTime before = LocalDateTime.now().minusDays(Math.max(days, 1));
        return operLogMapper.delete(new LambdaQueryWrapper<SysOperLog>()
                .lt(SysOperLog::getOperTime, before));
    }
}
