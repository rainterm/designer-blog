package com.designer.admin.module.blog.model.dto;

import com.designer.admin.common.core.PageQuery;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 评论分页查询入参。
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CommentQueryDTO extends PageQuery {

    @ApiModelProperty("所属文章 ID，为空表示不限")
    private Long postId;

    @ApiModelProperty("评论人昵称，模糊匹配")
    private String nickname;

    @ApiModelProperty("状态：0待审 1通过 2拒绝，为空表示不限")
    private Integer status;

    @ApiModelProperty("关键字，匹配评论内容")
    private String keyword;
}
