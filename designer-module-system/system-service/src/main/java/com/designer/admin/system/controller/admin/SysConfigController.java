package com.designer.admin.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.R;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.system.model.dto.ConfigQueryDTO;
import com.designer.admin.system.model.dto.ConfigSaveDTO;
import com.designer.admin.system.model.entity.SysConfig;
import com.designer.admin.system.service.SysConfigService;
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

/**
 * 参数配置
 */
@Valid
@Controller
@Mapping("/admin-api/system/config")
public class SysConfigController {

    @Inject
    private SysConfigService configService;

    @Get
    @Mapping("/page")
    @SaCheckPermission("system:config:list")
    public R<PageResult<SysConfig>> page(ConfigQueryDTO query) {
        return R.ok(PageResult.of(configService.page(query)));
    }

    @Get
    @Mapping("/{id}")
    @SaCheckPermission("system:config:query")
    public R<SysConfig> detail(Long id) {
        return R.ok(configService.getById(id));
    }

    @Get
    @Mapping("/value/{configKey}")
    public R<String> value(String configKey) {
        return R.ok(configService.getValue(configKey));
    }

    @Post
    @Mapping("/add")
    @OperLog(title = "参数设置", businessType = BusinessType.INSERT)
    @SaCheckPermission("system:config:add")
    public R<Void> add(@Body ConfigSaveDTO dto) {
        configService.save(dto);
        return R.ok();
    }

    @Put
    @Mapping("/update")
    @OperLog(title = "参数设置", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:config:edit")
    public R<Void> update(@Body ConfigSaveDTO dto) {
        configService.update(dto);
        return R.ok();
    }

    @Delete
    @Mapping("/remove")
    @OperLog(title = "参数设置", businessType = BusinessType.DELETE)
    @SaCheckPermission("system:config:remove")
    public R<Void> remove(String ids) {
        configService.remove(parseIds(ids));
        return R.ok();
    }

    @Delete
    @Mapping("/refresh")
    @OperLog(title = "参数设置", businessType = BusinessType.CLEAN)
    @SaCheckPermission("system:config:edit")
    public R<Void> refresh() {
        configService.refreshCache();
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
