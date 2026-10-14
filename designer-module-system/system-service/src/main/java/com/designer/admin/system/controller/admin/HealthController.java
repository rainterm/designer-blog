package com.designer.admin.system.controller.admin;

import cn.dev33.satoken.annotation.SaIgnore;
import com.designer.admin.common.core.R;
import org.noear.solon.Solon;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Mapping;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查
 */
@Controller
@Mapping("/admin-api/system")
public class HealthController {

    /**
     * 健康探针：返回应用名、Solon 版本、运行状态
     */
    @SaIgnore
    @Get
    @Mapping("/health")
    public R<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("app", "designer-server");
        data.put("solon", Solon.version());
        data.put("status", "UP");
        return R.ok(data);
    }
}