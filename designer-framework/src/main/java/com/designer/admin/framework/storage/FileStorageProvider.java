package com.designer.admin.framework.storage;

import org.noear.solon.core.handle.UploadedFile;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * 文件存储 SPI：新增存储方式时实现本接口并注册为 Solon 组件即可。
 */
public interface FileStorageProvider {

    /** 类型标识，写入库表 storage_type，如 local / s3 */
    String type();

    /** 前端展示名称 */
    String label();

    /** 类型专属配置字段（动态表单） */
    List<StorageFieldMeta> configFields();

    /** 校验类型专属配置，非法时抛 BizException */
    void validateConfig(Map<String, Object> props);

    /** 上传 */
    void store(UploadedFile file, String filePath, String contentType, StorageRuntimeConfig config);

    /** 删除物理文件（忽略不存在） */
    void delete(String filePath, StorageRuntimeConfig config);

    /** 打开文件流，调用方负责关闭 */
    InputStream open(String filePath, StorageRuntimeConfig config) throws Exception;

    /** 生成对外访问 URL */
    String buildAccessUrl(String filePath, StorageRuntimeConfig config);
}
