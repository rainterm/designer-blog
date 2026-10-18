package com.designer.admin.module.blog.model.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 后台编辑评论正文。
 *
 * <p>只允许改 {@code content}：昵称、身份（author_type）、IP 归属地都是「当时的客观事实」，
 * 事后改写会让评论区失去可信度。</p>
 *
 * @author 雨夜终端
 */
@Data
public class CommentUpdateDTO {

    @ApiModelProperty(value = "被编辑的评论 ID", required = true)
    private Long id;

    @ApiModelProperty(value = "新的正文内容", required = true)
    private String content;
}
