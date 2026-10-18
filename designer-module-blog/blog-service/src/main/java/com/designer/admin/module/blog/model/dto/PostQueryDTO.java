package com.designer.admin.module.blog.model.dto;

import com.designer.admin.common.core.PageQuery;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文章分页查询入参。
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PostQueryDTO extends PageQuery {

    @ApiModelProperty("标题，模糊匹配")
    private String title;

    @ApiModelProperty("关键字，匹配标题与正文")
    private String keyword;

    @ApiModelProperty("所属分类 ID，为空表示不限")
    private Long categoryId;

    @ApiModelProperty("所属标签 ID，为空表示不限")
    private Long tagId;

    @ApiModelProperty("状态：0草稿 1已发布 2已下线，为空表示不限")
    private Integer status;

    @ApiModelProperty("是否置顶：0否 1是，为空表示不限")
    private Integer isTop;
}
