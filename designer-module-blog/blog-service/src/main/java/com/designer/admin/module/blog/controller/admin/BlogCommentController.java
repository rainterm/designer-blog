package com.designer.admin.module.blog.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.PageQuery;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.R;
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

    /**
     * 待审批评论数：侧栏「评论管理」菜单红点。
     *
     * <p>权限跟列表页一致（{@code blog:comment:list}）：看得到菜单的人才需要这个数字，
     * 也不额外引入一个只服务于红点的权限点。</p>
     */
    @Get
    @Mapping("/pending-count")
    @SaCheckPermission("blog:comment:list")
    public R<Long> pendingCount() {
        return R.ok(commentService.countPending());
    }

    /**
     * 评论树（后台）：两层结构，与前台展示逻辑一致。
     *
     * <p>与 {@code /page} 的分工：{@code /page} 是扁平分页，适合「评论管理」列表页的
     * 筛选与批量操作；本接口按顶级评论分页并带上其全部回复，供文章编辑页还原层级。
     * 两者都保留：把 {@code /page} 改成树形会破坏列表页的筛选能力。</p>
     */
    @Get
    @Mapping("/tree")
    @SaCheckPermission("blog:comment:list")
    public R<PageResult<AdminCommentVO>> tree(Long postId, PageQuery query) {
        return R.ok(commentService.adminTreePage(postId, query));
    }

    @Put
    @Mapping("/audit")
    @OperLog(title = "博客评论", businessType = BusinessType.UPDATE)
    @SaCheckPermission("blog:comment:audit")
    public R<Void> audit(@Body @Validated CommentAuditDTO dto) {
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
    public R<Void> reply(@Body @Validated CommentReplyDTO dto) {
        commentService.reply(dto);
        return R.ok();
    }

    /**
     * 管理员以博主身份发表顶级评论（自己开一个话题，不挂在任何评论下）。
     */
    @Post
    @Mapping("/add")
    @OperLog(title = "博客评论", businessType = BusinessType.INSERT)
    @SaCheckPermission("blog:comment:add")
    public R<Void> add(@Body @Validated CommentAddDTO dto) {
        commentService.addByOwner(dto);
        return R.ok();
    }

    /**
     * 管理员编辑评论正文。
     */
    @Put
    @Mapping("/update")
    @OperLog(title = "博客评论", businessType = BusinessType.UPDATE)
    @SaCheckPermission("blog:comment:edit")
    public R<Void> update(@Body @Validated CommentUpdateDTO dto) {
        commentService.updateContent(dto);
        return R.ok();
    }

    /**
     * 置顶 / 取消置顶评论（影响前台与后台的展示顺序）。
     */
    @Put
    @Mapping("/top")
    @OperLog(title = "博客评论", businessType = BusinessType.UPDATE)
    @SaCheckPermission("blog:comment:top")
    public R<Void> top(@Body @Validated CommentTopDTO dto) {
        commentService.setTop(dto);
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
