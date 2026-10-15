package com.designer.admin.module.blog.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

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

    /**
     * 站长评论署名（保留昵称）。
     *
     * <p>后台回复评论时的默认署名。前台只用于占位等展示，
     * 昵称校验用的是下面的 {@link #reservedNicknames}。</p>
     */
    private String ownerNickname;

    /**
     * 完整的保留昵称名单（后台配置 + 站长署名），由服务端下发。
     *
     * <p>前台用它在提交前给出即时提示，**不再自己维护一份名单**：两边各留一份最容易
     * 漂移 —— 后端加了词、前端没加，用户就会填完才被打回。这里的值与
     * {@code BlogCommentService} 实际拦截用的名单同源（{@code NicknameUtil#reserved}）。</p>
     *
     * <p>匹配规则是「昵称**包含**其中任一词即拒绝」（忽略大小写与空白、全角半角等价），
     * 前后端同一套；前端只负责比对，规则本身以服务端为准。</p>
     *
     * <p>公开它不构成风险：这些词本来就通过错误提示暴露给用户，且真正的拦截在服务端。</p>
     */
    private List<String> reservedNicknames;
}
