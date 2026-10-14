package com.designer.admin.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.R;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.system.model.dto.JobLogQueryDTO;
import com.designer.admin.system.model.entity.SysJobLog;
import com.designer.admin.system.service.SysJobLogService;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Delete;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.validation.annotation.Valid;

/**
 * 定时任务调度日志
 */
@Valid
@Controller
@Mapping("/admin-api/system/joblog")
public class SysJobLogController {

    @Inject
    private SysJobLogService jobLogService;

    @Get
    @Mapping("/page")
    @SaCheckPermission("system:joblog:list")
    public R<PageResult<SysJobLog>> page(JobLogQueryDTO query) {
        return R.ok(PageResult.of(jobLogService.page(query)));
    }

    @Delete
    @Mapping("/remove")
    @OperLog(title = "调度日志", businessType = BusinessType.DELETE)
    @SaCheckPermission("system:joblog:remove")
    public R<Void> remove(String ids) {
        jobLogService.removeByIds(ids);
        return R.ok();
    }

    @Delete
    @Mapping("/clear")
    @OperLog(title = "调度日志", businessType = BusinessType.CLEAN)
    @SaCheckPermission("system:joblog:remove")
    public R<Void> clear() {
        jobLogService.clear();
        return R.ok();
    }

    @Delete
    @Mapping("/clear/{jobId}")
    @OperLog(title = "调度日志", businessType = BusinessType.CLEAN)
    @SaCheckPermission("system:joblog:remove")
    public R<Void> clearByJob(Long jobId) {
        jobLogService.clearByJobId(jobId);
        return R.ok();
    }
}
