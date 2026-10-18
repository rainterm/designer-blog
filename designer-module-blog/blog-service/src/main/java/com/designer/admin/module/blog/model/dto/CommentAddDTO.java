package com.designer.admin.module.blog.model.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 后台以博主身份发表顶级评论。
 *
 * <p>与 {@link CommentReplyDTO} 的区别：回复必须指定父评论，这里不指定，
 * 用于站长自己开一个话题（落到 {@code parent_id = 0}）。</p>
 *
 * @author 雨夜终端
 */
@Data
public class CommentAddDTO {

    @ApiModelProperty(value = "评论所属文章 ID", required = true)
    private Long postId;

    @ApiModelProperty(value = "评论内容", required = true)
    private String content;
}
