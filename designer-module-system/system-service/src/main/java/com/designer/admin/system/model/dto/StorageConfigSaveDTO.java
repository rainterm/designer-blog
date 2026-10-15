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
    /**
     * 下载 URL 前缀。**允许留空**：留空表示沿用服务端默认值
     * （见 {@code SysStorageConfigService#fillEntity} 的 {@code blankToDefault}），
     * 前端的存储配置表单也没有把它标成必填，所以这里不能加 {@code @NotBlank}。
     */
    private String urlPrefix;
    private String allowedExtensions;
    private Long maxSize;
    /** 类型专属配置（含密钥，序列化时整段脱敏） */
    @Sensitive
    private Map<String, Object> config;
    private String remark;
}
