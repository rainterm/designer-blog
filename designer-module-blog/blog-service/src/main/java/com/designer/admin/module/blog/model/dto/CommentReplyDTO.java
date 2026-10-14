package com.designer.admin.module.blog.model.dto;

import lombok.Data;

/**
 * 后台回复评论
 */
@Data
public class CommentReplyDTO {

    /** 被回复的评论ID */
    private Long id;

    /** 回复内容 */
    private String content;

    /** 回复人昵称；为空时使用默认的「博主」 */
    private String nickname;
}
