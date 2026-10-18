package com.designer.admin.framework.message;

/**
 * 站内信投递 SPI：由 system 模块实现，framework 的 inbox Provider 仅依赖此接口。
 *
 * @author 雨夜终端
 */
public interface InboxSink {

    /**
     * 发布站内通知。
     *
     * @param title     标题
     * @param content   内容
     * @param receivers 接收人用户 ID 逗号分隔；空串表示全体
     * @return 通知 ID
     */
    Long publish(String title, String content, String receivers);

    /**
     * 发布站内通知（带前端跳转路径）。
     *
     * @param title     标题
     * @param content   内容
     * @param receivers 接收人用户 ID 逗号分隔；空串表示全体
     * @param link      前端路由 path，如 /system/user；空则不跳转
     * @return 通知 ID
     */
    default Long publish(String title, String content, String receivers, String link) {
        return publish(title, content, receivers);
    }
}
