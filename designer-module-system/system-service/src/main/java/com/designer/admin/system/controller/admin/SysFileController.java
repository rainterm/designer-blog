package com.designer.admin.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.R;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.system.model.dto.FileQueryDTO;
import com.designer.admin.system.model.entity.SysFile;
import com.designer.admin.system.service.SysFileService;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Delete;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.validation.annotation.Valid;

/**
 * 文件管理：分页查询、详情、删除
 */
@Valid
@Controller
@Mapping("/admin-api/system/file")
public class SysFileController {

    @Inject
    private SysFileService fileService;

    @Get
    @Mapping("/page")
    @SaCheckPermission("system:file:list")
    public R<PageResult<SysFile>> page(FileQueryDTO query) {
        return R.ok(PageResult.of(fileService.page(query)));
    }

    @Get
    @Mapping("/{id}")
    @SaCheckPermission("system:file:query")
    public R<SysFile> detail(Long id) {
        return R.ok(fileService.getById(id));
    }

    @Delete
    @Mapping("/remove")
    @OperLog(title = "文件管理", businessType = BusinessType.DELETE)
    @SaCheckPermission("system:file:remove")
    public R<Void> remove(String ids) {
        fileService.removeByIds(ids);
        return R.ok();
    }
}
