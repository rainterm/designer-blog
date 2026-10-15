package com.designer.admin.module.blog.controller.app;

import cn.dev33.satoken.annotation.SaIgnore;
import com.designer.admin.common.core.PageQuery;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.R;
import com.designer.admin.common.core.ResultCode;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.framework.ratelimit.RateLimit;
import com.designer.admin.framework.ratelimit.RateLimitDimension;
import com.designer.admin.module.blog.constant.BlogCacheTags;
import com.designer.admin.module.blog.model.dto.PostQueryDTO;
import com.designer.admin.module.blog.model.dto.PublicCommentSaveDTO;
import com.designer.admin.module.blog.model.dto.VisitReportDTO;
import com.designer.admin.module.blog.model.entity.BlogCategory;
import com.designer.admin.module.blog.model.entity.BlogLink;
import com.designer.admin.module.blog.model.entity.BlogTag;
import com.designer.admin.module.blog.model.vo.MyCommentVO;
import com.designer.admin.module.blog.model.vo.PublicCommentVO;
import com.designer.admin.module.blog.model.vo.PublicPostVO;
import com.designer.admin.module.blog.model.vo.PublicSiteVO;
import com.designer.admin.module.blog.service.BlogCategoryService;
import com.designer.admin.module.blog.service.BlogCommentService;
import com.designer.admin.module.blog.service.BlogLinkService;
import com.designer.admin.module.blog.service.BlogPostService;
import com.designer.admin.module.blog.service.BlogSettingService;
import com.designer.admin.module.blog.service.BlogTagService;
import com.designer.admin.module.blog.service.BlogVisitService;
import org.noear.solon.annotation.Body;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Delete;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Post;
import org.noear.solon.core.handle.Context;
import org.noear.solon.data.annotation.Cache;
import org.noear.solon.validation.annotation.Valid;
import org.noear.solon.validation.annotation.Validated;

import java.util.List;

/**
 * 博客公开接口（免登录）
 */
@SaIgnore
@Valid
@Controller
@Mapping("/app-api/blog/public")
public class BlogPublicController {

    /** 评论人匿名标识的请求头名（与前端 blog-web 保持一致） */
    private static final String COMMENTER_KEY_HEADER = "X-Commenter-Key";

    /**
     * 公开读缓存的兜底存活时间（秒）：1 小时。
     */
    private static final int CACHE_SECONDS = 3600;

    @Inject
    private BlogPostService postService;
    @Inject
    private BlogCategoryService categoryService;
    @Inject
    private BlogTagService tagService;
    @Inject
    private BlogCommentService commentService;
    @Inject
    private BlogLinkService linkService;
    @Inject
    private BlogSettingService settingService;
    @Inject
    private BlogVisitService visitService;

    @Get
    @Mapping("/site")
    @Cache(tags = BlogCacheTags.SITE, seconds = CACHE_SECONDS)
    public R<PublicSiteVO> site() {
        return R.ok(settingService.getPublic());
    }

    @Get
    @Mapping("/posts")
    public R<PageResult<PublicPostVO>> posts(PostQueryDTO query, String categorySlug, String tagSlug) {
        if (categorySlug != null && !categorySlug.isBlank()) {
            BlogCategory cat = categoryService.getBySlug(categorySlug);
            if (cat == null || cat.getStatus() == null || cat.getStatus() != 1) {
                throw new BizException(ResultCode.NOT_FOUND, "分类不存在");
            }
            query.setCategoryId(cat.getId());
        }
        if (tagSlug != null && !tagSlug.isBlank()) {
            BlogTag tag = tagService.getBySlug(tagSlug);
            if (tag == null || tag.getStatus() == null || tag.getStatus() != 1) {
                throw new BizException(ResultCode.NOT_FOUND, "标签不存在");
            }
            query.setTagId(tag.getId());
        }
        return R.ok(PageResult.of(postService.publicPage(query)));
    }

    @Get
    @Mapping("/posts/{slug}")
    public R<PublicPostVO> postDetail(String slug) {
        return R.ok(postService.publicDetailBySlug(slug));
    }

    @Get
    @Mapping("/categories")
    @Cache(tags = BlogCacheTags.CATEGORY, seconds = CACHE_SECONDS)
    public R<List<BlogCategory>> categories() {
        return R.ok(categoryService.listEnabled());
    }

    @Get
    @Mapping("/tags")
    @Cache(tags = BlogCacheTags.TAG, seconds = CACHE_SECONDS)
    public R<List<BlogTag>> tags() {
        return R.ok(tagService.listEnabled());
    }

    @Get
    @Mapping("/links")
    @Cache(tags = BlogCacheTags.LINK, seconds = CACHE_SECONDS)
    public R<List<BlogLink>> links() {
        return R.ok(linkService.listEnabled());
    }

    /** 已通过评论（按顶级评论分页，每条带完整子树） */
    @Get
    @Mapping("/posts/{id}/comments")
    public R<PageResult<PublicCommentVO>> comments(Long id, PageQuery query) {
        return R.ok(commentService.listApprovedPage(id, query));
    }

    /**
     * 我的待审评论（当前文章）。
     *
     * <p>评论人身份靠请求头 {@value #COMMENTER_KEY_HEADER} 传递，而不是 query/body：
     * 它等同于管理自己评论的凭证，放 URL 里会进访问日志、浏览器历史和 Referer。</p>
     */
    @Get
    @Mapping("/posts/{id}/comments/mine")
    public R<List<MyCommentVO>> myComments(Long id) {
        return R.ok(commentService.listMyComments(id, commenterKey()));
    }

    /** 删除自己的评论（同样用请求头识别身份） */
    @Delete
    @Mapping("/comments/mine/{id}")
    public R<Void> removeMyComment(Long id) {
        commentService.deleteMine(id, commenterKey());
        return R.ok();
    }

    /**
     * 提交评论（匿名）。
     *
     * <p>两条规则共用一个 {@code name}、按声明顺序校验：间隔在前，
     * 被间隔挡下的突发请求就不会白吃掉日配额。</p>
     *
     * <p>限流先于方法体执行，所以它挡在验证码校验之前：连验证码都没填的垃圾请求
     * 同样会被限速。代价是**填错验证码也占一次额度**（本接口的 DTO 没写校验约束，
     * 因此不会先被参数校验拦下）。这是「把限流放在 API 边界」的必然结果，
     * 也正是它比放在 service 里更严的地方。</p>
     */
    @Post
    @Mapping("/comments")
    @RateLimit(name = "blog:comment-submit", dimension = RateLimitDimension.IP, rules = {
            @RateLimit.Rule(count = 1, window = 30, message = "评论太频繁了，请 {} 秒后再试"),
            @RateLimit.Rule(count = 20, window = 24 * 60 * 60,
                    message = "24 小时内评论条数已达上限，请稍后再试")
    })
    public R<Void> submitComment(@Body @Validated PublicCommentSaveDTO dto) {
        commentService.submitPublic(dto);
        return R.ok();
    }

    /**
     * 访问埋点（匿名）。
     *
     * <p>永远返回成功：埋点是附加能力，让访客因为「统计失败」看到报错毫无意义，
     * 服务端也已在 {@code BlogVisitService#record} 里吞掉异常。前端据此可以
     * fire-and-forget 上报，不必处理失败分支。</p>
     *
     * <p>只接收路径，IP / UA / 来源 / 设备 / 归属分类一律由服务端从请求推导，
     * 前端无法伪造统计。</p>
     */
    @Post
    @Mapping("/visit")
    public R<Void> visit(@Body @Validated VisitReportDTO dto) {
        visitService.record(dto);
        return R.ok();
    }

    private String commenterKey() {
        Context ctx = Context.current();
        return ctx == null ? null : ctx.header(COMMENTER_KEY_HEADER);
    }
}
