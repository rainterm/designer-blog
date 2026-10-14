package com.designer.admin.module.blog.model.dto;

import lombok.Data;

@Data
public class PublicCommentSaveDTO {

    private Long postId;

    private Long parentId;

    private String nickname;

    private String email;

    private String content;

    /** 验证码标识（由 /comment/captcha 下发，一次性使用） */
    private String captchaUuid;

    /** 验证码答案 */
    private String captchaCode;

    /**
     * 评论人匿名标识（前端 localStorage 里的随机密钥）。
     * 用于让评论人查看 / 删除自己的评论；缺失时评论仍可提交，但作者无法自行管理。
     */
    private String commenterKey;
}
