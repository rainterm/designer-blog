package com.designer.admin.module.blog.model.dto;

import lombok.Data;

@Data
public class LinkSaveDTO {
    private Long id;
    private String name;
    private String url;
    private String icon;
    private Integer sort;
    private Integer status;
    private String remark;
}
