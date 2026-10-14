package com.designer.admin.system.model.dto;

import com.designer.admin.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 存储配置分页查询
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StorageConfigQueryDTO extends PageQuery {

    private String configName;
    private String storageType;
    private Integer status;
}
