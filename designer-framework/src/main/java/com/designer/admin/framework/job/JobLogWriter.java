package com.designer.admin.framework.job;

/**
 * 定时任务日志写入 SPI：由 system 模块实现。
 */
public interface JobLogWriter {

    void write(JobLogRecord record);
}
