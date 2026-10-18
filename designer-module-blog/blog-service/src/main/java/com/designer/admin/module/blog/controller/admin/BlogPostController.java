package com.designer.admin.module.blog.controller.admin;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.Result;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.module.blog.model.dto.PostQueryDTO;
import com.designer.admin.module.blog.model.dto.PostPublishDTO;
import com.designer.admin.module.blog.model.dto.PostSaveDTO;
import com.designer.admin.module.blog.model.entity.BlogPost;
import com.designer.admin.module.blog.service.BlogPostService;
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
 * 文章后台接口。
 *
 * @author 雨夜终端
 */
@Valid
@Controller
@Mapping("/admin-api/blog/post")
@Api(tags = "博客文章")
public class BlogPostController {

    @Inject
    private BlogPostService postService;

    @Get
    @Mapping("/page")
    @SaCheckPermission("blog:post:list")
    @ApiOperation(value = "分页查询文章")
    public Result<PageResult<BlogPost>> page(PostQueryDTO query) {
        return Result.ok(PageResult.of(postService.listPage(query)));
    }

    @Get
    @Mapping("/{id}")
    @SaCheckPermission("blog:post:query")
    @ApiOperation(value = "获取文章详情")
    public Result<BlogPost> detail(Long id) {
        return Result.ok(postService.getById(id));
    }

    @Post
    @Mapping("/add")
    @OperLog(title = "博客文章", businessType = BusinessType.INSERT)
    @SaCheckPermission("blog:post:add")
    @ApiOperation(value = "新增文章")
    public Result<Void> add(@Body @Validated PostSaveDTO dto) {
        postService.add(dto);
        return Result.ok();
    }

    @Put
    @Mapping("/update")
    @OperLog(title = "博客文章", businessType = BusinessType.UPDATE)
    @SaCheckPermission("blog:post:edit")
    @ApiOperation(value = "修改文章")
    public Result<Void> update(@Body @Validated PostSaveDTO dto) {
        postService.update(dto);
        return Result.ok();
    }

    @Put
    @Mapping("/{id}/publish")
    @OperLog(title = "博客文章", businessType = BusinessType.UPDATE)
    @SaCheckPermission("blog:post:publish")
    @ApiOperation(value = "发布文章")
    public Result<Void> publish(Long id, @Body PostPublishDTO dto) {
        postService.publish(id, dto == null || dto.shouldPublish());
        return Result.ok();
    }

    @Delete
    @Mapping("/remove")
    @OperLog(title = "博客文章", businessType = BusinessType.DELETE)
    @SaCheckPermission("blog:post:remove")
    @ApiOperation(value = "删除文章")
    public Result<Void> remove(List<Long> ids) {
        postService.remove(ids);
        return Result.ok();
    }
}
