package com.designer.admin.module.blog.controller.admin;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.Result;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.module.blog.model.dto.LinkQueryDTO;
import com.designer.admin.module.blog.model.dto.LinkSaveDTO;
import com.designer.admin.module.blog.model.entity.BlogLink;
import com.designer.admin.module.blog.service.BlogLinkService;
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
 * 友情链接后台接口。
 *
 * @author 雨夜终端
 */
@Valid
@Controller
@Mapping("/admin-api/blog/link")
@Api(tags = "友链管理")
public class BlogLinkController {

    @Inject
    private BlogLinkService linkService;

    @Get
    @Mapping("/page")
    @SaCheckPermission("blog:link:list")
    @ApiOperation(value = "分页查询友链")
    public Result<PageResult<BlogLink>> page(LinkQueryDTO query) {
        return Result.ok(PageResult.of(linkService.listPage(query)));
    }

    @Get
    @Mapping("/{id}")
    @SaCheckPermission("blog:link:query")
    @ApiOperation(value = "获取友链详情")
    public Result<BlogLink> detail(Long id) {
        return Result.ok(linkService.getById(id));
    }

    @Post
    @Mapping("/add")
    @OperLog(title = "博客链接", businessType = BusinessType.INSERT)
    @SaCheckPermission("blog:link:add")
    @ApiOperation(value = "新增友链")
    public Result<Void> add(@Body @Validated LinkSaveDTO dto) {
        linkService.add(dto);
        return Result.ok();
    }

    @Put
    @Mapping("/update")
    @OperLog(title = "博客链接", businessType = BusinessType.UPDATE)
    @SaCheckPermission("blog:link:edit")
    @ApiOperation(value = "修改友链")
    public Result<Void> update(@Body @Validated LinkSaveDTO dto) {
        linkService.update(dto);
        return Result.ok();
    }

    @Delete
    @Mapping("/remove")
    @OperLog(title = "博客链接", businessType = BusinessType.DELETE)
    @SaCheckPermission("blog:link:remove")
    @ApiOperation(value = "删除友链")
    public Result<Void> remove(List<Long> ids) {
        linkService.remove(ids);
        return Result.ok();
    }
}
