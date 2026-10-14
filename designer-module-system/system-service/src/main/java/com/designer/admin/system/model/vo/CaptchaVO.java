package com.designer.admin.system.model.vo;

import lombok.Data;

/**
 * 登录验证码
 */
@Data
public class CaptchaVO {

    /** 是否启用验证码 */
    private boolean enabled;

    /** 验证码唯一标识（启用时有值） */
    private String uuid;

    /** Base64 图片（data:image/png;base64,...） */
    private String img;
}
