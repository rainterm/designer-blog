package com.designer.admin.system.job;

import org.noear.solon.annotation.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 示例任务 Bean，页面 invoke_target 可填写：
 * <ul>
 *   <li>demoTask.noParams</li>
 *   <li>demoTask.withParams('hello')</li>
 * </ul>
 */
@Component("demoTask")
public class DemoTask {

    private static final Logger log = LoggerFactory.getLogger(DemoTask.class);

    public void noParams() {
        log.info("[demoTask] 执行无参方法");
    }

    public void withParams(String params) {
        log.info("[demoTask] 执行有参方法：{}", params);
    }
}
