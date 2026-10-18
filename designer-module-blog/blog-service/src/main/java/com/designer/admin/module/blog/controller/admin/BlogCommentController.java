package com.designer.admin.module.blog.controller.admin;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.PageQuery;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.Result;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.module.blog.model.dto.CommentAddDTO;
import com.designer.admin.module.blog.model.dto.CommentAuditDTO;
import com.designer.admin.module.blog.model.dto.CommentQueryDTO;
import com.designer.admin.module.blog.model.dto.CommentReplyDTO;
import com.designer.admin.module.blog.model.dto.CommentTopDTO;
import com.designer.admin.module.blog.model.dto.CommentUpdateDTO;
import com.designer.admin.module.blog.model.entity.BlogComment;
import com.designer.admin.module.blog.model.vo.AdminCommentVO;
import com.designer.admin.module.blog.service.BlogCommentService;
import org.noear.solon.annotation.Body;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Delete;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Post;
import org.noear.solon.annotation.Put;
import org.noear.solon.validation.annotation.Valid;
import org.noear.solon.validation.annotation.Validated;

import java.util.List;

/**
 * 评论后台接口。
 *
 * @author 雨夜终端
 */
@Valid
@Controller
@Mapping("/admin-api/blog/comment")
@Api(tags = "博客评论")
public class BlogCommentController {

    @Inject
    private BlogCommentService commentService;

    @Get
    @Mapping("/page")
    @SaCheckPermission("blog:comment:list")
    @ApiOperation(value = "分页查询评论")
    public Result<PageResult<BlogComment>> page(CommentQueryDTO query) {
        return Result.ok(PageResult.of(commentService.listPage(query)));
    }

    @Get
    @Mapping("/pending-count")
    @SaCheckPermission("blog:comment:list")
    @ApiOperation(value = "统计待审核评论数量")
    public Result<Long> pendingCount() {
        return Result.ok(commentService.countPending());
    }

    @Get
    @Mapping("/tree")
    @SaCheckPermission("blog:comment:list")
    @ApiOperation(value = "查询文章评论树")
    public Result<PageResult<AdminCommentVO>> tree(Long postId, PageQuery query) {
        return Result.ok(commentService.listAdminTreePage(postId, query));
    }

    @Put
    @Mapping("/audit")
    @OperLog(title = "博客评论", businessType = BusinessType.UPDATE)
    @SaCheckPermission("blog:comment:audit")
    @ApiOperation(value = "审核评论")
    public Result<Void> audit(@Body @Validated CommentAuditDTO dto) {
        commentService.audit(dto);
        return Result.ok();
    }

    @Delete
    @Mapping("/remove")
    @OperLog(title = "博客评论", businessType = BusinessType.DELETE)
    @SaCheckPermission("blog:comment:remove")
    @ApiOperation(value = "删除评论")
    public Result<Void> remove(List<Long> ids) {
        commentService.remove(ids);
        return Result.ok();
    }

    @Post
    @Mapping("/reply")
    @OperLog(title = "博客评论", businessType = BusinessType.INSERT)
    @SaCheckPermission("blog:comment:reply")
    @ApiOperation(value = "回复评论")
    public Result<Void> reply(@Body @Validated CommentReplyDTO dto) {
        commentService.reply(dto);
        return Result.ok();
    }

    @Post
    @Mapping("/add")
    @OperLog(title = "博客评论", businessType = BusinessType.INSERT)
    @SaCheckPermission("blog:comment:add")
    @ApiOperation(value = "新增评论")
    public Result<Void> add(@Body @Validated CommentAddDTO dto) {
        commentService.addByOwner(dto);
        return Result.ok();
    }

    @Put
    @Mapping("/update")
    @OperLog(title = "博客评论", businessType = BusinessType.UPDATE)
    @SaCheckPermission("blog:comment:edit")
    @ApiOperation(value = "修改评论")
    public Result<Void> update(@Body @Validated CommentUpdateDTO dto) {
        commentService.updateContent(dto);
        return Result.ok();
    }

    @Put
    @Mapping("/top")
    @OperLog(title = "博客评论", businessType = BusinessType.UPDATE)
    @SaCheckPermission("blog:comment:top")
    @ApiOperation(value = "置顶或取消置顶评论")
    public Result<Void> top(@Body @Validated CommentTopDTO dto) {
        commentService.updateTop(dto);
        return Result.ok();
    }
}
