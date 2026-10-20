package com.designer.admin.module.blog.model.vo;

import com.designer.admin.common.core.DateTimePattern;
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
    @ONodeAttr(format = DateTimePattern.DATE_TIME)
    private LocalDateTime publishedAt;

    /**
     * 最后更新时间。
     *
     * <p>取自 {@code blog_post.update_time}（MyBatis-Plus 自动填充），只在文章真正被修改时变化：
     * 阅读数回写走的是 {@code update(null, wrapper)}，不会触发自动填充，所以浏览不会让它变。</p>
     */
    @ONodeAttr(format = DateTimePattern.DATE_TIME)
    private LocalDateTime updateTime;

    /**
     * 是否需要向前台提示「更新于」。
     *
     * <p>判定放在服务端而不是前端，是因为它依赖两个时间字段的先后关系，
     * 前端为了判断得先把「yyyy-MM-dd HH:mm:ss」重新解析成时间对象 —— 那等于把
     * 「后端负责时间语义、前端只负责展示」这条约定又打破一次。前端直接按这个布尔值决定
     * 要不要渲染 {@link #updateTime} 即可，不做任何比较。</p>
     *
     * <p>刚发布时 {@code update_time} 与 {@code published_at} 是同一秒（见
     * {@code AutoFillHandler} 与 {@code applyPublishFields}），差值为 0，
     * 此时展示「更新于」纯属噪音；只有真正编辑过（差值超过阈值）才为 true。</p>
     */
    private Boolean updated;
}
