package com.designer.admin.module.demo.controller.admin;

import com.designer.admin.common.core.R;
import com.designer.admin.module.demo.service.DemoService;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;

import java.util.Map;

/**
 * Demo 示例接口。二次开发可复制本模块后改包名与路径。
 */
@Controller
@Mapping("/admin-api/demo")
public class DemoController {

    @Inject
    private DemoService demoService;

    /** 连通性探测：GET /admin-api/demo/ping */
    @Get
    @Mapping("/ping")
    public R<Map<String, Object>> ping() {
        return R.ok(demoService.ping());
    }
}
