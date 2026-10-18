package com.designer.admin.module.blog.model.vo;

import lombok.Data;
import org.noear.snack4.annotation.ONodeAttr;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 公开文章列表/详情 VO
 *
 * @author 雨夜终端
 */
@Data
public class PublicPostVO {

    /** 文章ID */
    private Long id;

    /** 文章标题 */
    private String title;

    /** 文章别名，用于前台 URL */
    private String slug;

    /** 摘要，列表页展示 */
    private String summary;

    /** 正文（Markdown 原文；列表接口不返回，仅详情有值） */
    private String content;

    /** 封面图地址 */
    private String coverUrl;

    /** 所属分类ID */
    private Long categoryId;

    /** 所属分类名称 */
    private String categoryName;

    /** 所属分类别名，用于分类页链接 */
    private String categorySlug;

    /** 作者昵称 */
    private String authorName;

    /** 是否置顶：1是 0否 */
    private Integer isTop;

    /** 是否允许评论：1允许 0关闭 */
    private Integer allowComment;

    /** 阅读数 */
    private Integer viewCount;

    /** SEO 标题，空时前台回退为文章标题 */
    private String seoTitle;

    /** SEO 关键词，逗号分隔 */
    private String seoKeywords;

    /** SEO 描述，空时前台回退为摘要 */
    private String seoDescription;

    /** 标签名称列表 */
    private List<String> tagNames;

    /** 标签别名列表，用于标签页链接 */
    private List<String> tagSlugs;

    /** 发布时间，未发布为空 */
    @ONodeAttr(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishedAt;
}
