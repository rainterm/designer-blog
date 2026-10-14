package com.designer.admin.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.R;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.system.model.dto.DeptSaveDTO;
import com.designer.admin.system.model.entity.SysDept;
import com.designer.admin.system.service.SysDeptService;
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

/**
 * 部门管理
 */
@Valid
@Controller
@Mapping("/admin-api/system/dept")
public class SysDeptController {

    @Inject
    private SysDeptService deptService;

    @Get
    @Mapping("/tree")
    @SaCheckPermission("system:dept:list")
    public R<List<SysDept>> tree(String deptName, Integer status) {
        return R.ok(deptService.tree(deptName, status));
    }

    @Get
    @Mapping("/options")
    public R<List<SysDept>> options() {
        return R.ok(deptService.options());
    }

    @Get
    @Mapping("/{id}")
    @SaCheckPermission("system:dept:query")
    public R<SysDept> detail(Long id) {
        return R.ok(deptService.getById(id));
    }

    @Post
    @Mapping("/add")
    @OperLog(title = "部门管理", businessType = BusinessType.INSERT)
    @SaCheckPermission("system:dept:add")
    public R<Void> add(@Body DeptSaveDTO dto) {
        deptService.save(dto);
        return R.ok();
    }

    @Put
    @Mapping("/update")
    @OperLog(title = "部门管理", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:dept:edit")
    public R<Void> update(@Body DeptSaveDTO dto) {
        deptService.update(dto);
        return R.ok();
    }

    @Delete
    @Mapping("/{id}")
    @OperLog(title = "部门管理", businessType = BusinessType.DELETE)
    @SaCheckPermission("system:dept:remove")
    public R<Void> remove(Long id) {
        deptService.remove(id);
        return R.ok();
    }
}
