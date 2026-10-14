package com.designer.admin.framework.log;

/**
 * 操作日志写入 SPI：由 system 模块实现。
 */
public interface OperLogWriter {

    /** 异步落库，不阻塞主请求 */
    void writeAsync(OperLogRecord record);
}
