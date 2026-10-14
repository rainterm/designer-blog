package com.designer.admin.common.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 全局异步执行器（JDK25 虚拟线程），用于日志落库等非阻塞旁路任务
 */
public final class AsyncRunner {

    private static final ExecutorService EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private AsyncRunner() {
    }

    public static void run(Runnable task) {
        EXECUTOR.submit(() -> {
            try {
                task.run();
            } catch (Exception ignored) {
                // 旁路任务失败不外泄
            }
        });
    }
}
