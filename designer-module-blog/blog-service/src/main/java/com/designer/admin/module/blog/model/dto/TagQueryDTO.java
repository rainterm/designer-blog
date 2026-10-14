package com.designer.admin.module.blog.model.dto;

import com.designer.admin.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TagQueryDTO extends PageQuery {
    private String name;
    private String slug;
    private Integer status;
}
