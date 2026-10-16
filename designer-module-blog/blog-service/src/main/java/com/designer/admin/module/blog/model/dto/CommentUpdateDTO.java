package com.designer.admin.module.blog.model.dto;

import lombok.Data;

/**
 * 后台编辑评论正文。
 *
 * <p>只允许改 {@code content}：昵称、身份（author_type）、IP 归属地都是「当时的客观事实」，
 * 事后改写会让评论区失去可信度。</p>
 */
@Data
public class CommentUpdateDTO {

    /** 被编辑的评论ID */
    private Long id;

    /** 新的正文内容 */
    private String content;
}
