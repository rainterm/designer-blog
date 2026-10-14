package com.designer.admin.framework.storage;

/**
 * 存储配置数据源 SPI：由 system 模块实现。
 */
public interface StorageConfigSource {

    StorageRuntimeConfig requireDefaultRuntime();

    StorageRuntimeConfig requireRuntimeById(Long configId);
}
