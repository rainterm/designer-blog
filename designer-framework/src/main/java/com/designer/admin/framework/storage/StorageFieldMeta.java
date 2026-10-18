package com.designer.admin.framework.storage;

import lombok.Builder;
import lombok.Data;

/**
 * 存储类型配置项元数据（供前端动态渲染表单，新增 Provider 时自动扩展）
 *
 * @author 雨夜终端
 */
@Data
@Builder
public class StorageFieldMeta {

    /** 字段名（写入 configJson 的 key） */
    private String name;

    /** 显示标签 */
    private String label;

    /** 控件类型：text / password / number / switch / textarea */
    private String inputType;

    /** 是否必填 */
    private boolean required;

    /** 占位提示 */
    private String placeholder;

    /** 默认值（字符串形式，前端自行转换） */
    private String defaultValue;

    /** 帮助说明 */
    private String help;
}
