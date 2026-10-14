package com.designer.admin.module.blog.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.R;
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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Valid
@Controller
@Mapping("/admin-api/blog/link")
public class BlogLinkController {

    @Inject
    private BlogLinkService linkService;

    @Get
    @Mapping("/page")
    @SaCheckPermission("blog:link:list")
    public R<PageResult<BlogLink>> page(LinkQueryDTO query) {
        return R.ok(PageResult.of(linkService.page(query)));
    }

    @Get
    @Mapping("/{id}")
    @SaCheckPermission("blog:link:query")
    public R<BlogLink> detail(Long id) {
        return R.ok(linkService.getById(id));
    }

    @Post
    @Mapping("/add")
    @OperLog(title = "博客链接", businessType = BusinessType.INSERT)
    @SaCheckPermission("blog:link:add")
    public R<Void> add(@Body LinkSaveDTO dto) {
        linkService.save(dto);
        return R.ok();
    }

    @Put
    @Mapping("/update")
    @OperLog(title = "博客链接", businessType = BusinessType.UPDATE)
    @SaCheckPermission("blog:link:edit")
    public R<Void> update(@Body LinkSaveDTO dto) {
        linkService.update(dto);
        return R.ok();
    }

    @Delete
    @Mapping("/remove")
    @OperLog(title = "博客链接", businessType = BusinessType.DELETE)
    @SaCheckPermission("blog:link:remove")
    public R<Void> remove(String ids) {
        linkService.remove(parseIds(ids));
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
