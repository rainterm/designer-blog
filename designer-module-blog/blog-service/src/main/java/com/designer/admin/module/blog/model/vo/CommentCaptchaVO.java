package com.designer.admin.module.blog.model.vo;

import lombok.Data;

/**
 * 公开评论验证码
 */
@Data
public class CommentCaptchaVO {

    /** 验证码标识：提交评论时原样回传 */
    private String uuid;

    /** 验证码图片：data:image/png;base64,... */
    private String img;
}
