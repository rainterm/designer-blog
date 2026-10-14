package com.designer.admin.module.blog.model.dto;

import com.designer.admin.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PostQueryDTO extends PageQuery {
    private String title;
    private String keyword;
    private Long categoryId;
    private Long tagId;
    private Integer status;
    private Integer isTop;
}
