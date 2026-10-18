package com.designer.admin.module.blog.controller.app;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import cn.dev33.satoken.annotation.SaIgnore;
import com.designer.admin.module.blog.constant.BlogCommentConsts;
import com.designer.admin.common.core.PageQuery;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.Result;
import com.designer.admin.common.core.ResultCode;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.framework.ratelimit.RateLimit;
import com.designer.admin.framework.ratelimit.RateLimitDimension;
import com.designer.admin.module.blog.constant.BlogCacheConsts;
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
 *
 * @author 雨夜终端
 */
@SaIgnore
@Valid
@Controller
@Mapping("/app-api/blog/public")
@Api(tags = "博客前台")
public class BlogPublicController {

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
    @Cache(tags = BlogCacheConsts.SITE, seconds = BlogCacheConsts.CACHE_SECONDS)
    @ApiOperation(value = "获取站点配置")
    public Result<PublicSiteVO> getSite() {
        return Result.ok(settingService.getPublic());
    }

    @Get
    @Mapping("/posts")
    @ApiOperation(value = "分页查询文章")
    public Result<PageResult<PublicPostVO>> listPosts(PostQueryDTO query, String categorySlug, String tagSlug) {
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
        return Result.ok(PageResult.of(postService.listPublicPage(query)));
    }

    @Get
    @Mapping("/posts/{slug}")
    @ApiOperation(value = "获取文章详情")
    public Result<PublicPostVO> getPostDetail(String slug) {
        return Result.ok(postService.getPublicDetailBySlug(slug));
    }

    @Get
    @Mapping("/categories")
    @Cache(tags = BlogCacheConsts.CATEGORY, seconds = BlogCacheConsts.CACHE_SECONDS)
    @ApiOperation(value = "查询分类列表")
    public Result<List<BlogCategory>> listCategories() {
        return Result.ok(categoryService.listEnabled());
    }

    @Get
    @Mapping("/tags")
    @Cache(tags = BlogCacheConsts.TAG, seconds = BlogCacheConsts.CACHE_SECONDS)
    @ApiOperation(value = "查询标签列表")
    public Result<List<BlogTag>> listTags() {
        return Result.ok(tagService.listEnabled());
    }

    @Get
    @Mapping("/links")
    @Cache(tags = BlogCacheConsts.LINK, seconds = BlogCacheConsts.CACHE_SECONDS)
    @ApiOperation(value = "查询友链列表")
    public Result<List<BlogLink>> listLinks() {
        return Result.ok(linkService.listEnabled());
    }

    @Get
    @Mapping("/posts/{id}/comments")
    @ApiOperation(value = "分页查询文章评论")
    public Result<PageResult<PublicCommentVO>> listComments(Long id, PageQuery query) {
        return Result.ok(commentService.listApprovedPage(id, query));
    }

    @Get
    @Mapping("/posts/{id}/comments/mine")
    @ApiOperation(value = "查询我的评论")
    public Result<List<MyCommentVO>> listMyComments(Long id) {
        return Result.ok(commentService.listMyComments(id, commenterKey()));
    }

    @Delete
    @Mapping("/comments/{id}/mine")
    @ApiOperation(value = "删除我的评论")
    public Result<Void> removeMyComment(Long id) {
        commentService.removeMine(id, commenterKey());
        return Result.ok();
    }

    @Post
    @Mapping("/comments")
    @RateLimit(name = "blog:comment-submit", dimension = RateLimitDimension.IP, rules = {
            @RateLimit.Rule(count = 1, window = 30, message = "评论太频繁了，请 {} 秒后再试"),
            @RateLimit.Rule(count = 20, window = 24 * 60 * 60,
                    message = "24 小时内评论条数已达上限，请稍后再试")
    })
    @ApiOperation(value = "发表评论")
    public Result<Void> addComment(@Body @Validated PublicCommentSaveDTO dto) {
        commentService.addPublic(dto);
        return Result.ok();
    }

    @Post
    @Mapping("/visit")
    @ApiOperation(value = "上报访问记录")
    public Result<Void> addVisit(@Body @Validated VisitReportDTO dto) {
        visitService.addVisit(dto);
        return Result.ok();
    }

    private String commenterKey() {
        Context ctx = Context.current();
        return ctx == null ? null : ctx.header(BlogCommentConsts.COMMENTER_KEY_HEADER);
    }
}
