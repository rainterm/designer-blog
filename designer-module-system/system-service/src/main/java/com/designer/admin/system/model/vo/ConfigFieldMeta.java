package com.designer.admin.system.model.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 动态表单字段元数据（对外契约，不依赖 framework）。
 */
@Data
@Builder
public class ConfigFieldMeta {

    private String name;
    private String label;
    /** text / password / number / switch / textarea */
    private String inputType;
    private boolean required;
    private String placeholder;
    private String defaultValue;
    private String help;
}
