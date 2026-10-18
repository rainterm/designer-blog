package com.designer.admin.module.blog.controller.admin;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.Result;
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
import org.noear.solon.validation.annotation.Validated;

import java.util.List;

/**
 * 标签后台接口。
 *
 * @author 雨夜终端
 */
@Valid
@Controller
@Mapping("/admin-api/blog/tag")
@Api(tags = "博客标签")
public class BlogTagController {

    @Inject
    private BlogTagService tagService;

    @Get
    @Mapping("/page")
    @SaCheckPermission("blog:tag:list")
    @ApiOperation(value = "分页查询标签")
    public Result<PageResult<BlogTag>> page(TagQueryDTO query) {
        return Result.ok(PageResult.of(tagService.listPage(query)));
    }

    @Get
    @Mapping("/list")
    @SaCheckPermission("blog:tag:list")
    @ApiOperation(value = "查询标签列表")
    public Result<List<BlogTag>> list() {
        return Result.ok(tagService.listEnabled());
    }

    @Get
    @Mapping("/{id}")
    @SaCheckPermission("blog:tag:query")
    @ApiOperation(value = "获取标签详情")
    public Result<BlogTag> detail(Long id) {
        return Result.ok(tagService.getById(id));
    }

    @Post
    @Mapping("/add")
    @OperLog(title = "博客标签", businessType = BusinessType.INSERT)
    @SaCheckPermission("blog:tag:add")
    @ApiOperation(value = "新增标签")
    public Result<Void> add(@Body @Validated TagSaveDTO dto) {
        tagService.add(dto);
        return Result.ok();
    }

    @Put
    @Mapping("/update")
    @OperLog(title = "博客标签", businessType = BusinessType.UPDATE)
    @SaCheckPermission("blog:tag:edit")
    @ApiOperation(value = "修改标签")
    public Result<Void> update(@Body @Validated TagSaveDTO dto) {
        tagService.update(dto);
        return Result.ok();
    }

    @Delete
    @Mapping("/remove")
    @OperLog(title = "博客标签", businessType = BusinessType.DELETE)
    @SaCheckPermission("blog:tag:remove")
    @ApiOperation(value = "删除标签")
    public Result<Void> remove(List<Long> ids) {
        tagService.remove(ids);
        return Result.ok();
    }
}
