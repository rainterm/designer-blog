package com.designer.admin.system.model.dto;

import com.designer.admin.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 岗位查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PostQueryDTO extends PageQuery {
    private String postName;
    private String postCode;
    private Integer status;
}
