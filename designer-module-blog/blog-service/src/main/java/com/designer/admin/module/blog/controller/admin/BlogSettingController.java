package com.designer.admin.module.blog.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.R;
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

/**
 * 博客站点设置（单行）
 */
@Valid
@Controller
@Mapping("/admin-api/blog/setting")
public class BlogSettingController {

    @Inject
    private BlogSettingService settingService;

    @Get
    @Mapping("")
    @SaCheckPermission("blog:setting:query")
    public R<BlogSetting> detail() {
        return R.ok(settingService.get());
    }

    @Put
    @Mapping("")
    @OperLog(title = "站点设置", businessType = BusinessType.UPDATE)
    @SaCheckPermission("blog:setting:edit")
    public R<Void> update(@Body SettingSaveDTO dto) {
        settingService.update(dto);
        return R.ok();
    }
}
