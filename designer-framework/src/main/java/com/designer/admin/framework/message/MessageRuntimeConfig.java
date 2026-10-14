package com.designer.admin.framework.message;

import lombok.Builder;
import lombok.Data;

import java.util.Collections;
import java.util.Map;

/**
 * 消息通道运行时配置（来自 sys_message_config）
 */
@Data
@Builder
public class MessageRuntimeConfig {

    private Long configId;
    private String configName;
    private String channelType;
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

    public int propInt(String key, int defaultValue) {
        Object v = props == null ? null : props.get(key);
        if (v == null) {
            return defaultValue;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(v).trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
