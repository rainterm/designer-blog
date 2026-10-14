package com.designer.admin.module.blog.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 前台站点设置（公开接口返回）
 */
@Data
public class PublicSiteVO implements Serializable {

    /** 站点名称 */
    private String siteName;

    /** 站点副标题 */
    private String siteSubtitle;

    /** 站点 Logo 地址，空表示用内置 Logo */
    private String logoUrl;

    /** Logo 圆角（px） */
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
     * <p>前台据此决定是否渲染评论输入框；单篇文章的 {@code allowComment} 另行判断。</p>
     */
    private Integer commentEnabled;
}
