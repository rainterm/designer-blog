package com.designer.admin.module.blog.model.dto;

import lombok.Data;

@Data
public class TagSaveDTO {
    private Long id;
    private String name;
    private String slug;
    private Integer status;
}
