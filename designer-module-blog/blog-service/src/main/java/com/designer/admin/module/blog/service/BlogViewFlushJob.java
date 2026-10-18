package com.designer.admin.module.blog.service;

import com.designer.admin.module.blog.constant.BlogRedisKeyConsts;
import com.designer.admin.module.blog.constant.BlogViewConsts;
import com.designer.admin.module.blog.mapper.BlogPostMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.redisx.RedisClient;
import org.noear.redisx.plus.RedisLock;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Destroy;
import org.noear.solon.annotation.Init;
import org.noear.solon.annotation.Inject;

import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 阅读数定时回写：把 {@link BlogViewCounter} 里攒的增量合并进 {@code blog_post.view_count}。
 *
 * <p>阅读数的写回全靠它：N 次阅读先各自 {@code HINCRBY} 到 Redis，
 * 每隔 {@value com.designer.admin.module.blog.constant.BlogViewConsts#FLUSH_INTERVAL_SECONDS} 秒这里把它们按文章合并成一条
 * {@code UPDATE ... view_count + delta}，于是写次数从「每次阅读一次」降到「每篇文章每轮一次」。</p>
 *
 * <p>间隔的取舍：太短就退化成逐次写，太长则后台看到的数字滞后。
 * 30 秒对博客这种量级基本等于实时，而写入量约等于零。</p>
 *
 * <p>用 {@code @Init} + 单线程调度而不是数据驱动的 {@code sys_job} 表：这是计数链路的一部分，
 * 漏配一条任务记录就会导致阅读数永远停在 Redis 里不落库，不该依赖人工在后台启用。</p>
 *
 * @author 雨夜终端
 */
@Component
@Slf4j
public class BlogViewFlushJob {

    @Inject
    private BlogViewCounter viewCounter;
    @Inject
    private RedisClient redisClient;
    @Db
    private BlogPostMapper postMapper;

    private ScheduledExecutorService scheduler;

    @Init
    public void start() {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, r -> {
            Thread t = new Thread(r, "blog-view-flush");
            t.setDaemon(true);
            return t;
        });
        executor.setRemoveOnCancelPolicy(true);
        scheduler = executor;
        scheduler.scheduleWithFixedDelay(this::flushQuietly,
                BlogViewConsts.FLUSH_INTERVAL_SECONDS, BlogViewConsts.FLUSH_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    @Destroy
    public void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    /**
     * 回写一轮。
     *
     * <p>先抢锁：同一批增量被两个实例各加一次就会凭空翻倍，拿不到锁说明别的实例正在做，
     * 这一轮直接跳过即可（下一轮还会再来）。</p>
     *
     * @return 成功落库的文章篇数
     */
    public int flush() {
        RedisLock lock = redisClient.getLock(BlogRedisKeyConsts.POST_VIEW_FLUSH_LOCK);
        if (!lock.tryLock(BlogViewConsts.LOCK_LEASE_SECONDS)) {
            return 0;
        }
        try {
            return doFlush();
        } finally {
            lock.unLock();
        }
    }

    private int doFlush() {
        Map<Long, Long> batch = viewCounter.takeAll();
        int flushed = 0;
        for (Map.Entry<Long, Long> entry : batch.entrySet()) {
            Long postId = entry.getKey();
            long delta = entry.getValue();
            try {
                // 返回 0 也照样扣减：文章已被删除时这笔增量无处可去，留着会永远堆积
                postMapper.incrementViewCount(postId, delta);
                viewCounter.settle(postId, delta);
                flushed++;
            } catch (Exception e) {
                // 不回滚（即不扣减）：增量仍留在 Redis，下一轮重试。
                // 阅读数宁可晚一点，也不能丢
                log.warn("[blog-view] 回写失败 postId={} delta={}：{}", postId, delta, e.getMessage());
            }
        }
        return flushed;
    }

    private void flushQuietly() {
        try {
            flush();
        } catch (Exception e) {
            // Redis 抖动之类的异常不能让调度线程死掉（scheduleWithFixedDelay 会因未捕获异常停止后续执行）
            log.warn("[blog-view] 回写任务异常：{}", e.getMessage());
        }
    }
}
