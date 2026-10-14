package com.designer.admin.module.blog.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.R;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.module.blog.model.dto.TagQueryDTO;
import com.designer.admin.module.blog.model.dto.TagSaveDTO;
import com.designer.admin.module.blog.model.entity.BlogTag;
import com.designer.admin.module.blog.service.BlogTagService;
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
@Mapping("/admin-api/blog/tag")
public class BlogTagController {

    @Inject
    private BlogTagService tagService;

    @Get
    @Mapping("/page")
    @SaCheckPermission("blog:tag:list")
    public R<PageResult<BlogTag>> page(TagQueryDTO query) {
        return R.ok(PageResult.of(tagService.page(query)));
    }

    @Get
    @Mapping("/list")
    @SaCheckPermission("blog:tag:list")
    public R<List<BlogTag>> list() {
        return R.ok(tagService.listEnabled());
    }

    @Get
    @Mapping("/{id}")
    @SaCheckPermission("blog:tag:query")
    public R<BlogTag> detail(Long id) {
        return R.ok(tagService.getById(id));
    }

    @Post
    @Mapping("/add")
    @OperLog(title = "博客标签", businessType = BusinessType.INSERT)
    @SaCheckPermission("blog:tag:add")
    public R<Void> add(@Body TagSaveDTO dto) {
        tagService.save(dto);
        return R.ok();
    }

    @Put
    @Mapping("/update")
    @OperLog(title = "博客标签", businessType = BusinessType.UPDATE)
    @SaCheckPermission("blog:tag:edit")
    public R<Void> update(@Body TagSaveDTO dto) {
        tagService.update(dto);
        return R.ok();
    }

    @Delete
    @Mapping("/remove")
    @OperLog(title = "博客标签", businessType = BusinessType.DELETE)
    @SaCheckPermission("blog:tag:remove")
    public R<Void> remove(String ids) {
        tagService.remove(parseIds(ids));
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
