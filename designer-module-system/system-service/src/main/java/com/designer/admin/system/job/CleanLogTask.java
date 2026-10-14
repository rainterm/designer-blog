package com.designer.admin.system.job;

import com.designer.admin.system.service.SysJobLogService;
import com.designer.admin.system.service.SysLoginLogService;
import com.designer.admin.system.service.SysMessageRecordService;
import com.designer.admin.system.service.SysOperLogService;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 内置日志清理任务。页面 invoke_target 示例：
 * <ul>
 *   <li>cleanLogTask.cleanAll(30) — 清理 30 天前的全部日志</li>
 *   <li>cleanLogTask.cleanOperLog(30)</li>
 *   <li>cleanLogTask.cleanLoginLog(30)</li>
 *   <li>cleanLogTask.cleanJobLog(30)</li>
 *   <li>cleanLogTask.cleanMessageRecord(30)</li>
 * </ul>
 */
@Component("cleanLogTask")
public class CleanLogTask {

    private static final Logger log = LoggerFactory.getLogger(CleanLogTask.class);
    private static final int DEFAULT_DAYS = 30;

    @Inject
    private SysOperLogService operLogService;

    @Inject
    private SysLoginLogService loginLogService;

    @Inject
    private SysJobLogService jobLogService;

    @Inject
    private SysMessageRecordService messageRecordService;

    /** 清理全部日志表中超过保留天数的数据 */
    public void cleanAll(Integer days) {
        int keepDays = normalizeDays(days);
        int oper = operLogService.cleanOlderThan(keepDays);
        int login = loginLogService.cleanOlderThan(keepDays);
        int job = jobLogService.cleanOlderThan(keepDays);
        int msg = messageRecordService.cleanOlderThan(keepDays);
        log.info("[cleanLogTask] 清理完成 keepDays={} operLog={} loginLog={} jobLog={} msgRecord={}",
                keepDays, oper, login, job, msg);
    }

    public void cleanOperLog(Integer days) {
        int n = operLogService.cleanOlderThan(normalizeDays(days));
        log.info("[cleanLogTask] 操作日志清理 {} 条 (keepDays={})", n, normalizeDays(days));
    }

    public void cleanLoginLog(Integer days) {
        int n = loginLogService.cleanOlderThan(normalizeDays(days));
        log.info("[cleanLogTask] 登录日志清理 {} 条 (keepDays={})", n, normalizeDays(days));
    }

    public void cleanJobLog(Integer days) {
        int n = jobLogService.cleanOlderThan(normalizeDays(days));
        log.info("[cleanLogTask] 调度日志清理 {} 条 (keepDays={})", n, normalizeDays(days));
    }

    public void cleanMessageRecord(Integer days) {
        int n = messageRecordService.cleanOlderThan(normalizeDays(days));
        log.info("[cleanLogTask] 消息记录清理 {} 条 (keepDays={})", n, normalizeDays(days));
    }

    private static int normalizeDays(Integer days) {
        return days == null || days < 1 ? DEFAULT_DAYS : days;
    }
}
