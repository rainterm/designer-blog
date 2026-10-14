package com.designer.admin.system.model.dto;

import com.designer.admin.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典类型查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DictTypeQueryDTO extends PageQuery {
    private String dictName;
    private String dictType;
    private Integer status;
}
