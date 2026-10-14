package com.designer.admin.module.demo.service;

import org.noear.solon.annotation.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Demo 模块内部服务（不对外暴露；跨模块请依赖 {@link com.designer.admin.module.demo.api.DemoApi}）。
 */
@Component
public class DemoService {

    public Map<String, Object> ping() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("ok", true);
        data.put("module", "demo");
        return data;
    }
}
