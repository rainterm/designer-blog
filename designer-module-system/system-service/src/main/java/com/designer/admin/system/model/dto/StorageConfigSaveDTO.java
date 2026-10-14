package com.designer.admin.system.model.dto;

import com.designer.admin.common.annotation.Sensitive;
import lombok.Data;
import org.noear.solon.validation.annotation.NotBlank;

import java.util.Map;

/**
 * 存储配置保存
 */
@Data
public class StorageConfigSaveDTO {

    private Long id;
    @NotBlank(message = "配置名称不能为空")
    private String configName;
    @NotBlank(message = "存储类型不能为空")
    private String storageType;
    /** 是否设为默认 */
    private Integer isDefault;
    private Integer status;
    @NotBlank(message = "下载前缀不能为空")
    private String urlPrefix;
    private String allowedExtensions;
    private Long maxSize;
    /** 类型专属配置（含密钥，序列化时整段脱敏） */
    @Sensitive
    private Map<String, Object> config;
    private String remark;
}
