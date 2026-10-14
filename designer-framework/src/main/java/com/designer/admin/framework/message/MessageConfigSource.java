package com.designer.admin.framework.message;

/**
 * 消息通道配置数据源 SPI：由 system 模块实现。
 */
public interface MessageConfigSource {

    MessageRuntimeConfig requireDefaultRuntime(String channelType);

    MessageRuntimeConfig requireRuntimeById(Long configId);
}
