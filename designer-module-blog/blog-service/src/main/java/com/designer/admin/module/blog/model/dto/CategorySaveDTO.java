package com.designer.admin.module.blog.model.dto;

import lombok.Data;

@Data
public class CategorySaveDTO {
    private Long id;
    private String name;
    private String slug;
    private Integer sort;
    private Integer status;
    private String remark;
}
