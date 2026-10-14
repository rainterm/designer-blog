package com.designer.admin.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.R;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.system.model.dto.StorageConfigQueryDTO;
import com.designer.admin.system.model.dto.StorageConfigSaveDTO;
import com.designer.admin.system.model.entity.SysStorageConfig;
import com.designer.admin.system.model.vo.StorageTypeVO;
import com.designer.admin.system.service.SysStorageConfigService;
import org.noear.solon.annotation.Body;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Delete;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Post;
import org.noear.solon.annotation.Put;
import org.noear.solon.validation.annotation.Valid;

import java.util.List;
import java.util.Map;

/**
 * 文件存储配置管理（数据库配置，可扩展 Provider）
 */
@Valid
@Controller
@Mapping("/admin-api/system/storage")
public class SysStorageConfigController {

    @Inject
    private SysStorageConfigService storageConfigService;

    @Get
    @Mapping("/types")
    @SaCheckPermission("system:storage:list")
    public R<List<StorageTypeVO>> types() {
        return R.ok(storageConfigService.listTypes());
    }

    @Get
    @Mapping("/page")
    @SaCheckPermission("system:storage:list")
    public R<PageResult<SysStorageConfig>> page(StorageConfigQueryDTO query) {
        return R.ok(PageResult.of(storageConfigService.page(query)));
    }

    @Get
    @Mapping("/{id}")
    @SaCheckPermission("system:storage:query")
    public R<Map<String, Object>> detail(Long id) {
        return R.ok(storageConfigService.detailView(id));
    }

    @Post
    @Mapping("/add")
    @OperLog(title = "存储配置", businessType = BusinessType.INSERT)
    @SaCheckPermission("system:storage:add")
    public R<Void> add(@Body StorageConfigSaveDTO dto) {
        storageConfigService.save(dto);
        return R.ok();
    }

    @Put
    @Mapping("/update")
    @OperLog(title = "存储配置", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:storage:edit")
    public R<Void> update(@Body StorageConfigSaveDTO dto) {
        storageConfigService.update(dto);
        return R.ok();
    }

    @Delete
    @Mapping("/remove")
    @OperLog(title = "存储配置", businessType = BusinessType.DELETE)
    @SaCheckPermission("system:storage:remove")
    public R<Void> remove(String ids) {
        storageConfigService.removeByIds(ids);
        return R.ok();
    }

    @Put
    @Mapping("/{id}/default")
    @OperLog(title = "存储配置", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:storage:edit")
    public R<Void> setDefault(Long id) {
        storageConfigService.setDefault(id);
        return R.ok();
    }

    @Put
    @Mapping("/{id}/status")
    @OperLog(title = "存储配置", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:storage:edit")
    public R<Void> changeStatus(Long id, Integer status) {
        storageConfigService.changeStatus(id, status);
        return R.ok();
    }
}
