package com.designer.admin.module.blog.constant;

/**
 * 博客模块的 Redis 键（前缀）。
 *
 * <p>访问埋点与阅读数回写分属两个类，却共享同一批键：写入侧在 service、回写侧在定时任务，
 * 任一方改前缀而另一方没跟上，都会表现为「阅读数永远是 0」或「埋点写了没人读」。
 * 因此键只在这里定义一次，两边都引用常量。</p>
 *
 * @author 雨夜终端
 */
public final class BlogRedisKeyConsts {

    /** 访问埋点去重：{@code blog:visit:dedup:{visitorKey}:{path}}，时间窗内第二次起丢弃 */
    public static final String VISIT_DEDUP_PREFIX = "blog:visit:dedup:";

    /** 访客每日配额：{@code blog:visit:quota:{ip}}，值为当日已记录条数 */
    public static final String VISIT_QUOTA_PREFIX = "blog:visit:quota:";

    /** 待回写的阅读增量（Hash）：field = postId，value = 尚未落库的阅读次数 */
    public static final String POST_VIEW_PENDING = "blog:post:view:pending";

    /** 阅读增量回写任务的分布式锁：多实例部署时只让一个实例落库 */
    public static final String POST_VIEW_FLUSH_LOCK = "blog:post:view:flush:lock";

    private BlogRedisKeyConsts() {
    }
}
