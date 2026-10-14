package com.designer.admin.module.blog.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.designer.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 博客站点设置（单行，固定 id=1）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("blog_setting")
public class BlogSetting extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 站点名称 */
    private String siteName;

    /** 站点副标题（顶栏展示 / SEO 描述兜底） */
    private String siteSubtitle;

    /** 站点 Logo 地址，留空则前台使用内置 Logo */
    private String logoUrl;

    /** Logo 圆角（px，0 为直角） */
    private Integer logoRadius;

    /** 页脚版权 */
    private String copyright;

    /** ICP 备案号 */
    private String icp;

    /** ICP 备案号链接 */
    private String icpUrl;

    /** SEO 默认关键字（逗号分隔） */
    private String seoKeywords;

    /** SEO 默认描述 */
    private String seoDescription;

    /**
     * 全局评论总开关：1 开启，0 关闭。
     *
     * <p>与单篇文章的 {@code blog_post.allow_comment} 是「与」关系：
     * 只有两者都开启时，该文章才允许提交评论。</p>
     */
    private Integer commentEnabled;
}
