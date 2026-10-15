package com.designer.admin.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.R;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.system.model.dto.JobQueryDTO;
import com.designer.admin.system.model.dto.JobSaveDTO;
import com.designer.admin.system.model.entity.SysJob;
import com.designer.admin.system.service.SysJobService;
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

import java.util.Map;

/**
 * 定时任务管理
 */
@Valid
@Controller
@Mapping("/admin-api/system/job")
public class SysJobController {

    @Inject
    private SysJobService jobService;

    @Get
    @Mapping("/page")
    @SaCheckPermission("system:job:list")
    public R<PageResult<SysJob>> page(JobQueryDTO query) {
        return R.ok(PageResult.of(jobService.page(query)));
    }

    @Get
    @Mapping("/{id}")
    @SaCheckPermission("system:job:query")
    public R<Map<String, Object>> detail(Long id) {
        return R.ok(jobService.detailView(id));
    }

    @Post
    @Mapping("/add")
    @OperLog(title = "定时任务", businessType = BusinessType.INSERT)
    @SaCheckPermission("system:job:add")
    public R<Void> add(@Body @Validated JobSaveDTO dto) {
        jobService.save(dto);
        return R.ok();
    }

    @Put
    @Mapping("/update")
    @OperLog(title = "定时任务", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:job:edit")
    public R<Void> update(@Body @Validated JobSaveDTO dto) {
        jobService.update(dto);
        return R.ok();
    }

    @Delete
    @Mapping("/remove")
    @OperLog(title = "定时任务", businessType = BusinessType.DELETE)
    @SaCheckPermission("system:job:remove")
    public R<Void> remove(String ids) {
        jobService.removeByIds(ids);
        return R.ok();
    }

    @Put
    @Mapping("/{id}/status")
    @OperLog(title = "定时任务", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:job:edit")
    public R<Void> changeStatus(Long id, Integer status) {
        jobService.changeStatus(id, status);
        return R.ok();
    }

    @Put
    @Mapping("/{id}/run")
    @OperLog(title = "定时任务", businessType = BusinessType.OTHER)
    @SaCheckPermission("system:job:run")
    public R<Void> run(Long id) {
        jobService.runOnce(id);
        return R.ok();
    }
}
