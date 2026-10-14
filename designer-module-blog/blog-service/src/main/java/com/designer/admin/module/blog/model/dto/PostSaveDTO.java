package com.designer.admin.module.blog.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class PostSaveDTO {
    private Long id;
    private String title;
    private String slug;
    private String summary;
    private String content;
    private String coverUrl;
    private Long categoryId;
    private Integer status;
    private Integer isTop;
    private Integer allowComment;
    private String seoTitle;
    private String seoKeywords;
    private String seoDescription;
    private List<Long> tagIds;
}
