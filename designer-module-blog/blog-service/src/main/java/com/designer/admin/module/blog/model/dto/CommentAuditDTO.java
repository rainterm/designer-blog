package com.designer.admin.module.blog.model.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 评论审核入参。
 *
 * @author 雨夜终端
 */
@Data
public class CommentAuditDTO {

    @ApiModelProperty(value = "评论 ID", required = true)
    private Long id;

    @ApiModelProperty(value = "审核结果：1通过 2拒绝", required = true)
    private Integer status;
}
