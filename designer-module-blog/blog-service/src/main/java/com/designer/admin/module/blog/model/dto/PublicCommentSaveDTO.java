package com.designer.admin.module.blog.model.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 前台评论提交入参。
 *
 * @author 雨夜终端
 */
@Data
public class PublicCommentSaveDTO {

    @ApiModelProperty(value = "评论所属文章 ID", required = true)
    private Long postId;

    @ApiModelProperty("父评论 ID，顶级评论留空或传 0")
    private Long parentId;

    @ApiModelProperty(value = "评论人昵称", required = true)
    private String nickname;

    @ApiModelProperty("评论人邮箱，仅站长可见")
    private String email;

    @ApiModelProperty(value = "评论内容", required = true)
    private String content;

    @ApiModelProperty("滑块验证码的二次校验凭据，由 POST /app-api/captcha/check 下发，一次性有效")
    private String captchaVerification;

    @ApiModelProperty("评论人匿名标识（前端 localStorage 里的随机密钥），用于查看 / 删除自己的评论")
    private String commenterKey;
}
