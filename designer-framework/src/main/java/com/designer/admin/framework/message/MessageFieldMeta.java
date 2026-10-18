package com.designer.admin.framework.message;

import lombok.Builder;
import lombok.Data;

/**
 * 消息通道配置项元数据（供前端动态表单；新增 Provider 时自动扩展）
 *
 * @author 雨夜终端
 */
@Data
@Builder
public class MessageFieldMeta {

    private String name;
    private String label;
    /** text / password / number / switch / textarea */
    private String inputType;
    private boolean required;
    private String placeholder;
    private String defaultValue;
    private String help;
}
