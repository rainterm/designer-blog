package com.designer.admin.module.blog.model.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 分类新增 / 修改入参。
 *
 * @author 雨夜终端
 */
@Data
public class CategorySaveDTO {

    @ApiModelProperty("分类 ID，新增时留空")
    private Long id;

    @ApiModelProperty("分类名称")
    private String name;

    @ApiModelProperty("分类别名，用于前台 URL")
    private String slug;

    @ApiModelProperty("排序值，越小越靠前")
    private Integer sort;

    @ApiModelProperty("状态：1启用 0停用")
    private Integer status;

    @ApiModelProperty("备注")
    private String remark;
}
