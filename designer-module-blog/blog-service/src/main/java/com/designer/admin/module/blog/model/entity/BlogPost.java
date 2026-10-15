package com.designer.admin.module.blog.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.designer.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.noear.snack4.annotation.ONodeAttr;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 博客文章
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("blog_post")
public class BlogPost extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String slug;

    private String summary;

    private String content;

    private String coverUrl;

    private Long categoryId;

    private Long authorId;

    private String authorName;

    /** 0草稿 1已发布 */
    private Integer status;

    /** 1置顶 0否 */
    private Integer isTop;

    /** 1允许评论 0否 */
    private Integer allowComment;

    private Integer viewCount;

    private String seoTitle;

    private String seoKeywords;

    private String seoDescription;

    @ONodeAttr(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishedAt;

    @TableLogic
    @ONodeAttr(ignore = true)
    private Integer deleted;

    /** 分类名称（非库字段） */
    @TableField(exist = false)
    private String categoryName;

    /**
     * 分类 slug（非库字段）。
     *
     * <p>和 {@link #categoryName} 一起在 {@code fillExtras} 里批量填好。
     * 有了它，{@code toPublicVO} 就不必为了拿 slug 再单独查一次分类 ——
     * 那次查询在列表页是「每篇一条」，纯属重复。</p>
     */
    @TableField(exist = false)
    private String categorySlug;

    /** 标签 ID 列表（非库字段） */
    @TableField(exist = false)
    private List<Long> tagIds;

    /** 标签名称列表（非库字段） */
    @TableField(exist = false)
    private List<String> tagNames;

    /** 标签 slug 列表（非库字段），同样由 fillExtras 批量填好 */
    @TableField(exist = false)
    private List<String> tagSlugs;
}
