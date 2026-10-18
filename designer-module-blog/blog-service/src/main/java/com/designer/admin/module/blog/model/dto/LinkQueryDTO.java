package com.designer.admin.module.blog.model.dto;

import com.designer.admin.common.core.PageQuery;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 友情链接分页查询入参。
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LinkQueryDTO extends PageQuery {

    @ApiModelProperty("链接名称，模糊匹配")
    private String name;

    @ApiModelProperty("状态：1启用 0停用，为空表示不限")
    private Integer status;
}
