package com.designer.admin.module.blog.controller.admin;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.Result;
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

import java.util.List;

/**
 * 分类后台接口。
 *
 * @author 雨夜终端
 */
@Valid
@Controller
@Mapping("/admin-api/blog/category")
@Api(tags = "博客分类")
public class BlogCategoryController {

    @Inject
    private BlogCategoryService categoryService;

    @Get
    @Mapping("/page")
    @SaCheckPermission("blog:category:list")
    @ApiOperation(value = "分页查询分类")
    public Result<PageResult<BlogCategory>> page(CategoryQueryDTO query) {
        return Result.ok(PageResult.of(categoryService.listPage(query)));
    }

    @Get
    @Mapping("/list")
    @SaCheckPermission("blog:category:list")
    @ApiOperation(value = "查询分类列表")
    public Result<List<BlogCategory>> list() {
        return Result.ok(categoryService.listEnabled());
    }

    @Get
    @Mapping("/{id}")
    @SaCheckPermission("blog:category:query")
    @ApiOperation(value = "获取分类详情")
    public Result<BlogCategory> detail(Long id) {
        return Result.ok(categoryService.getById(id));
    }

    @Post
    @Mapping("/add")
    @OperLog(title = "博客分类", businessType = BusinessType.INSERT)
    @SaCheckPermission("blog:category:add")
    @ApiOperation(value = "新增分类")
    public Result<Void> add(@Body @Validated CategorySaveDTO dto) {
        categoryService.add(dto);
        return Result.ok();
    }

    @Put
    @Mapping("/update")
    @OperLog(title = "博客分类", businessType = BusinessType.UPDATE)
    @SaCheckPermission("blog:category:edit")
    @ApiOperation(value = "修改分类")
    public Result<Void> update(@Body @Validated CategorySaveDTO dto) {
        categoryService.update(dto);
        return Result.ok();
    }

    @Delete
    @Mapping("/remove")
    @OperLog(title = "博客分类", businessType = BusinessType.DELETE)
    @SaCheckPermission("blog:category:remove")
    @ApiOperation(value = "删除分类")
    public Result<Void> remove(List<Long> ids) {
        categoryService.remove(ids);
        return Result.ok();
    }
}
