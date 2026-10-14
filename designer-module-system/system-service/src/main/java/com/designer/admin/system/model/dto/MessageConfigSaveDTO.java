package com.designer.admin.system.model.dto;

import com.designer.admin.common.annotation.Sensitive;
import lombok.Data;

import java.util.Map;

@Data
public class MessageConfigSaveDTO {

    private Long id;
    private String configName;
    private String channelType;
    private Integer isDefault;
    private Integer status;
    /** 通道专属配置（含密钥，序列化时整段脱敏） */
    @Sensitive
    private Map<String, Object> config;
    private String remark;
}
