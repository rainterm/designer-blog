package com.designer.admin.module.blog.model.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 文章新增 / 修改入参。
 *
 * @author 雨夜终端
 */
@Data
public class PostSaveDTO {

    @ApiModelProperty("文章 ID，新增时留空")
    private Long id;

    @ApiModelProperty("标题")
    private String title;

    @ApiModelProperty("别名，用于前台 URL")
    private String slug;

    @ApiModelProperty("摘要，列表页展示")
    private String summary;

    @ApiModelProperty("正文内容，Markdown")
    private String content;

    @ApiModelProperty("封面图地址")
    private String coverUrl;

    @ApiModelProperty("所属分类 ID")
    private Long categoryId;

    @ApiModelProperty("状态：0草稿 1已发布 2已下线")
    private Integer status;

    @ApiModelProperty("是否置顶：0否 1是")
    private Integer isTop;

    @ApiModelProperty("是否允许评论：0否 1是")
    private Integer allowComment;

    @ApiModelProperty("SEO 标题")
    private String seoTitle;

    @ApiModelProperty("SEO 关键字，逗号分隔")
    private String seoKeywords;

    @ApiModelProperty("SEO 描述")
    private String seoDescription;

    @ApiModelProperty("关联的标签 ID 列表")
    private List<Long> tagIds;
}
