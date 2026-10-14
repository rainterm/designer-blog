package com.designer.admin.system.model.dto;

import com.designer.admin.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 参数配置查询
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ConfigQueryDTO extends PageQuery {
    private String configName;
    private String configKey;
    private String configType;
}
