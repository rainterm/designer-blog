package com.designer.admin.system.controller.open;

import com.designer.admin.common.core.R;
import com.designer.admin.framework.satoken.StpOAuthUtil;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Mapping;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 开放接口示例：需 OAuth2 token + Scope open.ping
 */
@Controller
@Mapping("/open-api/system")
public class OpenApiPingController {

    @Get
    @Mapping("/ping")
    public R<Map<String, Object>> ping() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("ok", true);
        data.put("clientId", StpOAuthUtil.getClientId());
        data.put("scopes", StpOAuthUtil.getScopes());
        data.put("serverTime", System.currentTimeMillis());
        return R.ok(data);
    }
}
