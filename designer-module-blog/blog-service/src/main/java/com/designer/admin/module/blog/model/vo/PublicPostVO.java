package com.designer.admin.module.blog.model.vo;

import lombok.Data;
import org.noear.snack4.annotation.ONodeAttr;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 公开文章列表/详情 VO
 */
@Data
public class PublicPostVO {
    private Long id;
    private String title;
    private String slug;
    private String summary;
    private String content;
    private String coverUrl;
    private Long categoryId;
    private String categoryName;
    private String categorySlug;
    private String authorName;
    private Integer isTop;
    private Integer allowComment;
    private Integer viewCount;
    private String seoTitle;
    private String seoKeywords;
    private String seoDescription;
    private List<String> tagNames;
    private List<String> tagSlugs;

    @ONodeAttr(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishedAt;
}
