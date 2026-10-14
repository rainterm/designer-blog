package com.designer.admin.system.model.dto;

import lombok.Data;
import org.noear.solon.validation.annotation.NotBlank;

/**
 * 参数配置保存
 */
@Data
public class ConfigSaveDTO {

    private Long id;

    @NotBlank(message = "参数名称不能为空")
    private String configName;

    @NotBlank(message = "参数键名不能为空")
    private String configKey;

    @NotBlank(message = "参数键值不能为空")
    private String configValue;

    /** Y内置 N自定义；新增默认 N */
    private String configType;

    private String remark;
}
