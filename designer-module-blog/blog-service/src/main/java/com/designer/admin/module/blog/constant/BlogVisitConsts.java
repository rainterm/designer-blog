package com.designer.admin.module.blog.constant;

/**
 * 访问埋点与统计的固定值：去重窗口、每日配额、统计区间与保留天数。
 *
 * <p>这些值是**运营口径**，不是随手写的魔法数字：去重窗口决定「刷新算不算一次阅读」，
 * 每日配额决定「被判定为刷量的阈值」，默认 / 最大统计天数决定后台图表的可选范围。
 * 集中在常量类里，调整口径时一处改完，写入侧与统计侧同时生效。</p>
 *
 * @author 雨夜终端
 */
public final class BlogVisitConsts {

    /** 去重窗口（秒）：覆盖刷新、重复上报与前端路由抖动 */
    public static final int DEDUP_TTL_SECONDS = 60;

    /** 单 IP 每日记录上限：正常阅读远达不到，只为挡住脚本刷量 */
    public static final int DAILY_QUOTA = 3000;

    /** 一天对应的秒数 */
    public static final int DAY_SECONDS = 24 * 60 * 60;

    /** 统计区间默认天数 */
    public static final int DEFAULT_DAYS = 30;

    /** 统计区间上限天数 */
    public static final int MAX_DAYS = 365;

    /** 排行榜默认取前 N 名 */
    public static final int TOP_SIZE = 10;

    /** 访问日志默认保留天数（清理任务未传参时使用） */
    public static final int DEFAULT_KEEP_DAYS = 90;

    private BlogVisitConsts() {
    }
}
