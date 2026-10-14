package com.designer.admin.framework.job;

/**
 * 定时任务常量
 */
public final class ScheduleConstants {

    private ScheduleConstants() {
    }

    public static final String TASK_CLASS_NAME = "TASK_CLASS_NAME";
    public static final String TASK_PROPERTIES = "TASK_PROPERTIES";

    /** 状态：正常 */
    public static final int STATUS_NORMAL = 1;
    /** 状态：暂停 */
    public static final int STATUS_PAUSE = 0;

    /** misfire：默认 */
    public static final int MISFIRE_DEFAULT = 1;
    /** misfire：立即执行 */
    public static final int MISFIRE_IGNORE_MISFIRES = 2;
    /** misfire：放弃执行 */
    public static final int MISFIRE_DO_NOTHING = 3;

    /** 允许并发 */
    public static final int CONCURRENT_ALLOW = 1;
    /** 禁止并发 */
    public static final int CONCURRENT_DISALLOW = 0;
}
