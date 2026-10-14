package com.designer.admin.framework.message;

import java.util.List;
import java.util.Map;

/**
 * 消息通道 SPI：新增短信 / 钉钉 / 微信 / 飞书时实现本接口并注册为组件即可。
 */
public interface MessageChannelProvider {

    /** 通道标识：email / inbox / sms / dingtalk / wechat / feishu */
    String type();

    /** 前端展示名称 */
    String label();

    /** 类型专属配置字段 */
    List<MessageFieldMeta> configFields();

    /** 校验配置 */
    void validateConfig(Map<String, Object> props);

    /** 发送消息 */
    void send(MessagePayload payload, MessageRuntimeConfig config);
}
