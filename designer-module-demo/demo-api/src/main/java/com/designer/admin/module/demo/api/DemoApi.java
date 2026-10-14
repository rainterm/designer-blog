package com.designer.admin.module.demo.api;

import java.util.Map;

/**
 * Demo 对外 API（按需暴露；其他模块只依赖 demo-api）。
 */
public interface DemoApi {

    /**
     * 连通性探测。
     */
    Map<String, Object> ping();
}
