package com.designer.admin.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.designer.admin.common.core.R;
import com.designer.admin.system.service.MonitorService;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;

import java.util.Map;

/**
 * 服务监控（首页仪表盘：总览 / 磁盘分区）
 */
@Controller
@Mapping("/admin-api/system/monitor")
@SaCheckLogin
public class MonitorController {

    @Inject
    private MonitorService monitorService;

    @Get
    @Mapping("/server")
    public R<Map<String, Object>> server() {
        return R.ok(monitorService.serverInfo());
    }
}
