package com.designer.admin.module.blog.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.R;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.module.blog.model.dto.CategoryQueryDTO;
import com.designer.admin.module.blog.model.dto.CategorySaveDTO;
import com.designer.admin.module.blog.model.entity.BlogCategory;
import com.designer.admin.module.blog.service.BlogCategoryService;
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
@Mapping("/admin-api/blog/category")
public class BlogCategoryController {

    @Inject
    private BlogCategoryService categoryService;

    @Get
    @Mapping("/page")
    @SaCheckPermission("blog:category:list")
    public R<PageResult<BlogCategory>> page(CategoryQueryDTO query) {
        return R.ok(PageResult.of(categoryService.page(query)));
    }

    @Get
    @Mapping("/list")
    @SaCheckPermission("blog:category:list")
    public R<List<BlogCategory>> list() {
        return R.ok(categoryService.listEnabled());
    }

    @Get
    @Mapping("/{id}")
    @SaCheckPermission("blog:category:query")
    public R<BlogCategory> detail(Long id) {
        return R.ok(categoryService.getById(id));
    }

    @Post
    @Mapping("/add")
    @OperLog(title = "博客分类", businessType = BusinessType.INSERT)
    @SaCheckPermission("blog:category:add")
    public R<Void> add(@Body @Validated CategorySaveDTO dto) {
        categoryService.save(dto);
        return R.ok();
    }

    @Put
    @Mapping("/update")
    @OperLog(title = "博客分类", businessType = BusinessType.UPDATE)
    @SaCheckPermission("blog:category:edit")
    public R<Void> update(@Body @Validated CategorySaveDTO dto) {
        categoryService.update(dto);
        return R.ok();
    }

    @Delete
    @Mapping("/remove")
    @OperLog(title = "博客分类", businessType = BusinessType.DELETE)
    @SaCheckPermission("blog:category:remove")
    public R<Void> remove(String ids) {
        categoryService.remove(parseIds(ids));
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
