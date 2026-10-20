package com.designer.admin.module.blog.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.designer.admin.common.core.BaseEntity;
import com.designer.admin.common.core.DateTimePattern;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.noear.snack4.annotation.ONodeAttr;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 博客文章
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("blog_post")
public class BlogPost extends BaseEntity {

    /** 文章标题 */
    private String title;

    /** 文章别名，用于前台 URL（唯一） */
    private String slug;

    /** 摘要，列表页展示 */
    private String summary;

    /** 正文（Markdown 原文，渲染在前端完成） */
    private String content;

    /** 封面图地址 */
    private String coverUrl;

    /** 所属分类ID */
    private Long categoryId;

    /** 作者用户ID */
    private Long authorId;

    /** 作者昵称（冗余列，避免列表页逐条查用户表） */
    private String authorName;

    /** 0草稿 1已发布 */
    private Integer status;

    /** 1置顶 0否 */
    private Integer isTop;

    /** 1允许评论 0否 */
    private Integer allowComment;

    /** 阅读数（由浏览埋点定时批量回写） */
    private Integer viewCount;

    /** SEO 标题，留空时前台回退为文章标题 */
    private String seoTitle;

    /** SEO 关键词，逗号分隔 */
    private String seoKeywords;

    /** SEO 描述，留空时前台回退为摘要 */
    private String seoDescription;

    /** 发布时间，草稿为空 */
    @ONodeAttr(format = DateTimePattern.DATE_TIME)
    private LocalDateTime publishedAt;

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
