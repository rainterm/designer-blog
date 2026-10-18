package com.designer.admin.module.blog.controller.admin;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.Result;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.module.blog.model.dto.SettingSaveDTO;
import com.designer.admin.module.blog.model.entity.BlogSetting;
import com.designer.admin.module.blog.service.BlogSettingService;
import org.noear.solon.annotation.Body;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Put;
import org.noear.solon.validation.annotation.Valid;
import org.noear.solon.validation.annotation.Validated;

/**
 * 博客站点设置（单行）
 *
 * @author 雨夜终端
 */
@Valid
@Controller
@Mapping("/admin-api/blog/setting")
@Api(tags = "博客设置")
public class BlogSettingController {

    @Inject
    private BlogSettingService settingService;

    @Get
    @Mapping("")
    @SaCheckPermission("blog:setting:query")
    @ApiOperation(value = "获取博客设置")
    public Result<BlogSetting> getDetail() {
        return Result.ok(settingService.getSetting());
    }

    @Put
    @Mapping("")
    @OperLog(title = "站点设置", businessType = BusinessType.UPDATE)
    @SaCheckPermission("blog:setting:edit")
    @ApiOperation(value = "修改博客设置")
    public Result<Void> update(@Body @Validated SettingSaveDTO dto) {
        settingService.update(dto);
        return Result.ok();
    }
}
