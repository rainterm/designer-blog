package com.designer.admin.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.PageQuery;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.R;
import com.designer.admin.system.model.entity.SysLoginLog;
import com.designer.admin.system.model.entity.SysOperLog;
import com.designer.admin.system.service.SysLoginLogService;
import com.designer.admin.system.service.SysOperLogService;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.validation.annotation.Valid;

/**
 * 日志管理（操作日志 + 登录日志，仅查询；清理由定时任务负责）
 */
@Valid
@Controller
@Mapping("/admin-api/system/log")
public class SysLogController {

    @Inject
    private SysOperLogService operLogService;

    @Inject
    private SysLoginLogService loginLogService;

    /**
     * 分页查询操作日志
     */
    @Get
    @Mapping("/oper/page")
    @SaCheckPermission("system:operlog:list")
    public R<PageResult<SysOperLog>> operPage(PageQuery query, String title, String operName, Integer status) {
        return R.ok(PageResult.of(operLogService.page(query, title, operName, status)));
    }

    /**
     * 分页查询登录日志
     */
    @Get
    @Mapping("/login/page")
    @SaCheckPermission("system:loginlog:list")
    public R<PageResult<SysLoginLog>> loginPage(PageQuery query, String username, Integer status) {
        return R.ok(PageResult.of(loginLogService.page(query, username, status)));
    }
}
