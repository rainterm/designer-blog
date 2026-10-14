package com.designer.admin.module.blog.model.dto;

import com.designer.admin.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class LinkQueryDTO extends PageQuery {
    private String name;
    private Integer status;
}
