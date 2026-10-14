package com.designer.admin.module.blog.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.R;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.module.blog.model.dto.CommentAuditDTO;
import com.designer.admin.module.blog.model.dto.CommentQueryDTO;
import com.designer.admin.module.blog.model.dto.CommentReplyDTO;
import com.designer.admin.module.blog.model.entity.BlogComment;
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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Valid
@Controller
@Mapping("/admin-api/blog/comment")
public class BlogCommentController {

    @Inject
    private BlogCommentService commentService;

    @Get
    @Mapping("/page")
    @SaCheckPermission("blog:comment:list")
    public R<PageResult<BlogComment>> page(CommentQueryDTO query) {
        return R.ok(PageResult.of(commentService.page(query)));
    }

    @Put
    @Mapping("/audit")
    @OperLog(title = "博客评论", businessType = BusinessType.UPDATE)
    @SaCheckPermission("blog:comment:audit")
    public R<Void> audit(@Body CommentAuditDTO dto) {
        commentService.audit(dto);
        return R.ok();
    }

    @Delete
    @Mapping("/remove")
    @OperLog(title = "博客评论", businessType = BusinessType.DELETE)
    @SaCheckPermission("blog:comment:remove")
    public R<Void> remove(String ids) {
        commentService.remove(parseIds(ids));
        return R.ok();
    }

    /**
     * 管理员回复评论：直接挂到目标评论下并以「已通过」公开。
     */
    @Post
    @Mapping("/reply")
    @OperLog(title = "博客评论", businessType = BusinessType.INSERT)
    @SaCheckPermission("blog:comment:reply")
    public R<Void> reply(@Body CommentReplyDTO dto) {
        commentService.reply(dto);
        return R.ok();
    }

    private List<Long> parseIds(String ids) {
        if (ids == null || ids.isBlank()) {
            return List.of();
        }
        return Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }
}
