package com.designer.admin.module.blog.model.dto;

import lombok.Data;

/**
 * 后台以博主身份发表顶级评论。
 *
 * <p>与 {@link CommentReplyDTO} 的区别：回复必须指定父评论，这里不指定，
 * 用于站长自己开一个话题（落到 {@code parent_id = 0}）。</p>
 */
@Data
public class CommentAddDTO {

    /** 评论所属文章ID */
    private Long postId;

    /** 评论内容 */
    private String content;
}
