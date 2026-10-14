package com.designer.admin.module.blog.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.R;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.module.blog.model.dto.PostQueryDTO;
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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Valid
@Controller
@Mapping("/admin-api/blog/post")
public class BlogPostController {

    @Inject
    private BlogPostService postService;

    @Get
    @Mapping("/page")
    @SaCheckPermission("blog:post:list")
    public R<PageResult<BlogPost>> page(PostQueryDTO query) {
        return R.ok(PageResult.of(postService.page(query)));
    }

    @Get
    @Mapping("/{id}")
    @SaCheckPermission("blog:post:query")
    public R<BlogPost> detail(Long id) {
        return R.ok(postService.getById(id));
    }

    @Post
    @Mapping("/add")
    @OperLog(title = "博客文章", businessType = BusinessType.INSERT)
    @SaCheckPermission("blog:post:add")
    public R<Void> add(@Body PostSaveDTO dto) {
        postService.save(dto);
        return R.ok();
    }

    @Put
    @Mapping("/update")
    @OperLog(title = "博客文章", businessType = BusinessType.UPDATE)
    @SaCheckPermission("blog:post:edit")
    public R<Void> update(@Body PostSaveDTO dto) {
        postService.update(dto);
        return R.ok();
    }

    @Put
    @Mapping("/publish/{id}")
    @OperLog(title = "博客文章", businessType = BusinessType.UPDATE)
    @SaCheckPermission("blog:post:publish")
    public R<Void> publish(Long id, @Body Map<String, Object> body) {
        boolean publish = true;
        if (body != null && body.get("publish") != null) {
            Object v = body.get("publish");
            if (v instanceof Boolean b) {
                publish = b;
            } else {
                publish = !"false".equalsIgnoreCase(String.valueOf(v)) && !"0".equals(String.valueOf(v));
            }
        }
        postService.publish(id, publish);
        return R.ok();
    }

    @Delete
    @Mapping("/remove")
    @OperLog(title = "博客文章", businessType = BusinessType.DELETE)
    @SaCheckPermission("blog:post:remove")
    public R<Void> remove(String ids) {
        postService.remove(parseIds(ids));
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
