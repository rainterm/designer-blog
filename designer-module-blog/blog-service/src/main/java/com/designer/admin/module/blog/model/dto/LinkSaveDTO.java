package com.designer.admin.module.blog.model.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 友情链接新增 / 修改入参。
 *
 * @author 雨夜终端
 */
@Data
public class LinkSaveDTO {

    @ApiModelProperty("链接 ID，新增时留空")
    private Long id;

    @ApiModelProperty("链接名称")
    private String name;

    @ApiModelProperty("跳转地址")
    private String url;

    @ApiModelProperty("站点图标地址")
    private String icon;

    @ApiModelProperty("排序值，越小越靠前")
    private Integer sort;

    @ApiModelProperty("状态：1启用 0停用")
    private Integer status;

    @ApiModelProperty("备注")
    private String remark;
}
