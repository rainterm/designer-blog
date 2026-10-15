package com.designer.admin.module.blog.model.dto;

import lombok.Data;

@Data
public class PublicCommentSaveDTO {

    private Long postId;

    private Long parentId;

    private String nickname;

    private String email;

    private String content;

    /**
     * 滑块验证码的二次校验凭据。
     *
     * <p>前端拖拽通过后调用 {@code POST /captcha/check} 得到，服务端再拿去复核一次
     * （见 {@code com.designer.admin.framework.captcha.CaptchaVerifier}）。**一次性**：校验通过后服务端就把它作废，
     * 所以同一个值不能重复提交。</p>
     */
    private String captchaVerification;

    /**
     * 评论人匿名标识（前端 localStorage 里的随机密钥）。
     * 用于让评论人查看 / 删除自己的评论；缺失时评论仍可提交，但作者无法自行管理。
     */
    private String commenterKey;
}
