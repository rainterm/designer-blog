package com.designer.admin.system.model.dto;

import lombok.Data;

import java.util.List;

/**
 * 测试 / 手动发送消息
 */
@Data
public class MessageSendDTO {

    /** 指定配置；为空则按 channelType 取默认 */
    private Long configId;

    /** 通道类型（configId 为空时必填） */
    private String channelType;

    private List<String> to;
    private List<String> cc;
    private String title;
    private String content;
    private Boolean html;
}
