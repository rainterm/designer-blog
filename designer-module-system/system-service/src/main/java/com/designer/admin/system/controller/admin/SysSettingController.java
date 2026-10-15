package com.designer.admin.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaIgnore;
import com.designer.admin.common.core.R;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.system.constant.SystemCacheTags;
import com.designer.admin.system.model.dto.SystemSettingSaveDTO;
import com.designer.admin.system.model.entity.SysSetting;
import com.designer.admin.system.model.vo.SystemSettingVO;
import com.designer.admin.system.service.SysSettingService;
import org.noear.solon.annotation.Body;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Put;
import org.noear.solon.data.annotation.Cache;
import org.noear.solon.validation.annotation.Valid;
import org.noear.solon.validation.annotation.Validated;

/**
 * 系统设置（单行）：系统名称 / 登录页副标题 / Logo / 默认偏好设置
 */
@Valid
@Controller
@Mapping("/admin-api/system/setting")
public class SysSettingController {

    /** 公开读缓存兜底存活时间（秒）：1 小时 */
    private static final int CACHE_SECONDS = 3600;

    @Inject
    private SysSettingService settingService;

    /**
     * 公开系统设置（免登录）。
     *
     * <p>登录页在拿到 token 前就要展示系统名称与 Logo，所以必须免登录；
     * 只下发系统名称 / Logo / 默认偏好这些不敏感字段。</p>
     */
    @SaIgnore
    @Get
    @Mapping("/public")
    @Cache(tags = SystemCacheTags.SETTING_PUBLIC, seconds = CACHE_SECONDS)
    public R<SystemSettingVO> publicSetting() {
        return R.ok(settingService.getPublic());
    }

    @Get
    @Mapping("")
    @SaCheckPermission("system:setting:query")
    public R<SysSetting> detail() {
        return R.ok(settingService.get());
    }

    @Put
    @Mapping("")
    @OperLog(title = "系统设置", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:setting:edit")
    public R<Void> update(@Body @Validated SystemSettingSaveDTO dto) {
        settingService.update(dto);
        return R.ok();
    }
}
