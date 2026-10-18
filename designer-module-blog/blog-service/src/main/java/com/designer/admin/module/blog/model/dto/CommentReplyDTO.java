package com.designer.admin.module.blog.model.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 后台回复评论。
 *
 * @author 雨夜终端
 */
@Data
public class CommentReplyDTO {

    @ApiModelProperty(value = "被回复的评论 ID", required = true)
    private Long id;

    @ApiModelProperty(value = "回复内容", required = true)
    private String content;
}
