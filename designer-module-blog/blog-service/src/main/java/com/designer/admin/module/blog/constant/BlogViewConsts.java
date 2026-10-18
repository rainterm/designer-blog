package com.designer.admin.module.blog.constant;

/**
 * 阅读数回写任务（{@code BlogViewCounter} → {@code BlogViewFlushJob}）的节拍常量。
 *
 * <p>回写间隔与锁租期是一对：租期必须明显大于一轮回写耗时，
 * 但又要足够短，保证实例崩溃后锁能自动释放、另一个实例接手。</p>
 *
 * @author 雨夜终端
 */
public final class BlogViewConsts {

    /** 回写间隔（秒） */
    public static final int FLUSH_INTERVAL_SECONDS = 30;

    /** 锁租期（秒）：远大于一轮回写耗时，实例崩溃后也能自动释放 */
    public static final int LOCK_LEASE_SECONDS = 60;

    private BlogViewConsts() {
    }
}
