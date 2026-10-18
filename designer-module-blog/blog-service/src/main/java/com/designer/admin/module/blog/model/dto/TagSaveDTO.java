package com.designer.admin.module.blog.model.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 标签新增 / 修改入参。
 *
 * @author 雨夜终端
 */
@Data
public class TagSaveDTO {

    @ApiModelProperty("标签 ID，新增时留空")
    private Long id;

    @ApiModelProperty("标签名称")
    private String name;

    @ApiModelProperty("标签别名，用于前台 URL")
    private String slug;

    @ApiModelProperty("状态：1启用 0停用")
    private Integer status;
}
