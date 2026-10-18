package com.designer.admin.module.blog.model.dto;

import com.designer.admin.common.core.PageQuery;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 标签分页查询入参。
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TagQueryDTO extends PageQuery {

    @ApiModelProperty("标签名称，模糊匹配")
    private String name;

    @ApiModelProperty("标签别名，模糊匹配")
    private String slug;

    @ApiModelProperty("状态：1启用 0停用，为空表示不限")
    private Integer status;
}
