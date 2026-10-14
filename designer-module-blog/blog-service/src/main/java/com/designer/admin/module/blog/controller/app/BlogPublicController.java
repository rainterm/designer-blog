package com.designer.admin.module.blog.controller.app;

import cn.dev33.satoken.annotation.SaIgnore;
import com.designer.admin.common.core.PageQuery;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.R;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.module.blog.model.dto.PostQueryDTO;
import com.designer.admin.module.blog.model.dto.PublicCommentSaveDTO;
import com.designer.admin.module.blog.model.dto.VisitReportDTO;
import com.designer.admin.module.blog.model.entity.BlogCategory;
import com.designer.admin.module.blog.model.entity.BlogLink;
import com.designer.admin.module.blog.model.entity.BlogTag;
import com.designer.admin.module.blog.model.vo.CommentCaptchaVO;
import com.designer.admin.module.blog.model.vo.MyCommentVO;
import com.designer.admin.module.blog.model.vo.PublicCommentVO;
import com.designer.admin.module.blog.model.vo.PublicPostVO;
import com.designer.admin.module.blog.model.vo.PublicSiteVO;
import com.designer.admin.module.blog.service.BlogCategoryService;import com.designer.admin.module.blog.service.BlogCommentService;
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
import org.noear.solon.validation.annotation.Valid;

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

    /** 站点设置（站点名 / Logo / 版权 / 备案号） */
    @Get
    @Mapping("/site")
    public R<PublicSiteVO> site() {
        return R.ok(settingService.getPublic());
    }

    @Get
    @Mapping("/posts")
    public R<PageResult<PublicPostVO>> posts(PostQueryDTO query, String categorySlug, String tagSlug) {
        if (categorySlug != null && !categorySlug.isBlank()) {
            BlogCategory cat = categoryService.getBySlug(categorySlug);
            if (cat == null || cat.getStatus() == null || cat.getStatus() != 1) {
                throw new BizException("分类不存在");
            }
            query.setCategoryId(cat.getId());
        }
        if (tagSlug != null && !tagSlug.isBlank()) {
            BlogTag tag = tagService.getBySlug(tagSlug);
            if (tag == null || tag.getStatus() == null || tag.getStatus() != 1) {
                throw new BizException("标签不存在");
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
    public R<List<BlogCategory>> categories() {
        return R.ok(categoryService.listEnabled());
    }

    @Get
    @Mapping("/tags")
    public R<List<BlogTag>> tags() {
        return R.ok(tagService.listEnabled());
    }

    @Get
    @Mapping("/links")
    public R<List<BlogLink>> links() {
        return R.ok(linkService.listEnabled());
    }

    /** 已通过评论（按顶级评论分页，每条带完整子树） */
    @Get
    @Mapping("/posts/{id}/comments")
    public R<PageResult<PublicCommentVO>> comments(Long id, PageQuery query) {
        return R.ok(commentService.listApprovedPage(id, query));
    }

    /** 评论验证码（匿名接口的防刷前置） */
    @Get
    @Mapping("/comment/captcha")
    public R<CommentCaptchaVO> commentCaptcha() {
        return R.ok(commentService.createCaptcha());
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

    @Post
    @Mapping("/comments")
    public R<Void> submitComment(@Body PublicCommentSaveDTO dto) {
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
    public R<Void> visit(@Body VisitReportDTO dto) {
        visitService.record(dto);
        return R.ok();
    }

    private String commenterKey() {
        Context ctx = Context.current();
        return ctx == null ? null : ctx.header(COMMENTER_KEY_HEADER);
    }
}
