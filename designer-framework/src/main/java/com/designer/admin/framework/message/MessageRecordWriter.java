package com.designer.admin.framework.message;

/**
 * 消息发送记录写入 SPI：由 system 模块实现。
 */
public interface MessageRecordWriter {

    void recordAsync(MessageRuntimeConfig config, MessagePayload payload,
                     boolean success, String errorMsg, long costMs);
}
