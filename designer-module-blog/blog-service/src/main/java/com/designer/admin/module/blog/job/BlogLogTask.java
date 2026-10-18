package com.designer.admin.module.blog.job;

import com.designer.admin.module.blog.service.BlogVisitService;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

/**
 * 博客内置日志清理任务。页面 invoke_target 示例：
 * <ul>
 *   <li>blogLogTask.cleanVisitLog(90) — 清理 90 天前的访问日志</li>
 *   <li>blogLogTask.cleanVisitLog(null) — 用默认保留期（90 天）</li>
 * </ul>
 *
 * <p>访问日志是明细表，会随流量持续增长；不清理的话既是存储负担，
 * 也会让聚合查询的扫描量越滚越大。</p>
 *
 * @author 雨夜终端
 */
@Component("blogLogTask")
@Slf4j
public class BlogLogTask {

    @Inject
    private BlogVisitService visitService;

    public void cleanVisitLog(Integer days) {
        int n = visitService.removeOlderThan(days);
        log.info("[blogLogTask] 访问日志清理 {} 条 (keepDays={})", n, days);
    }
}
