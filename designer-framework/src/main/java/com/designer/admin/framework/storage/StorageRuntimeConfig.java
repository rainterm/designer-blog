package com.designer.admin.framework.storage;

import lombok.Builder;
import lombok.Data;

import java.util.Collections;
import java.util.Map;

/**
 * 一次存储操作所用的运行时配置（来自数据库 sys_storage_config）
 *
 * @author 雨夜终端
 */
@Data
@Builder
public class StorageRuntimeConfig {

    private Long configId;
    private String configName;
    private String storageType;
    private String urlPrefix;
    private String allowedExtensions;
    private long maxSize;
    /** 是否公开访问：公开配置的 URL 走 /app-api，私有配置走 /admin-api */
    private boolean publicAccess;
    /** 类型专属配置（已解析的 JSON Map） */
    @Builder.Default
    private Map<String, Object> props = Collections.emptyMap();

    public String prop(String key) {
        Object v = props == null ? null : props.get(key);
        return v == null ? "" : String.valueOf(v);
    }

    public String prop(String key, String defaultValue) {
        String v = prop(key);
        return v == null || v.isBlank() ? defaultValue : v;
    }

    public boolean propBool(String key, boolean defaultValue) {
        Object v = props == null ? null : props.get(key);
        if (v == null) {
            return defaultValue;
        }
        if (v instanceof Boolean b) {
            return b;
        }
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(s) || "1".equals(s);
    }

    public long propLong(String key, long defaultValue) {
        Object v = props == null ? null : props.get(key);
        if (v == null) {
            return defaultValue;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(v).trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
