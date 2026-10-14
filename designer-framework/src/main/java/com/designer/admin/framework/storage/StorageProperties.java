package com.designer.admin.framework.storage;

import lombok.Data;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.annotation.Inject;

/**
 * 文件存储全局默认项（来自 app.yml {@code designer.storage.*}）
 */
@Data
@Configuration
public class StorageProperties {

    /**
     * 代理下载 URL 前缀，须与下载接口映射一致。
     * 例：{@code /admin-api/system/file/download/}
     */
    @Inject("${designer.storage.default-url-prefix}")
    private String defaultUrlPrefix;

    /**
     * 本地存储默认根目录。
     * 例：{@code ./data/uploads}
     */
    @Inject("${designer.storage.local-default-path}")
    private String localDefaultPath;

    /** 规范化后的下载前缀（保证以 / 结尾） */
    public String normalizedUrlPrefix() {
        String p = defaultUrlPrefix == null ? "" : defaultUrlPrefix.trim();
        if (p.isEmpty()) {
            throw new IllegalStateException("缺少配置 designer.storage.default-url-prefix");
        }
        return p.endsWith("/") ? p : p + "/";
    }

    public String resolvedLocalDefaultPath() {
        String p = localDefaultPath == null ? "" : localDefaultPath.trim();
        if (p.isEmpty()) {
            throw new IllegalStateException("缺少配置 designer.storage.local-default-path");
        }
        return p;
    }
}
