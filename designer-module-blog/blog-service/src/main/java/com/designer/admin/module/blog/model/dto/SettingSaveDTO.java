package com.designer.admin.module.blog.model.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 站点设置保存入参。
 *
 * @author 雨夜终端
 */
@Data
public class SettingSaveDTO {

    @ApiModelProperty("站点名称")
    private String siteName;

    @ApiModelProperty("站点副标题")
    private String siteSubtitle;

    @ApiModelProperty("Logo 地址")
    private String logoUrl;

    @ApiModelProperty("Logo 圆角像素，0 表示直角")
    private Integer logoRadius;

    @ApiModelProperty("站点主题色（#RRGGBB），留空或非法时回落默认")
    private String themeColor;

    @ApiModelProperty("版权信息")
    private String copyright;

    @ApiModelProperty("ICP 备案号")
    private String icp;

    @ApiModelProperty("ICP 备案查询地址")
    private String icpUrl;

    @ApiModelProperty("公安网安备案号")
    private String policeIcp;

    @ApiModelProperty("公安网安备案查询地址")
    private String policeIcpUrl;

    @ApiModelProperty("SEO 关键字，逗号分隔")
    private String seoKeywords;

    @ApiModelProperty("SEO 描述")
    private String seoDescription;

    @ApiModelProperty("是否开启评论：0否 1是")
    private Integer commentEnabled;

    @ApiModelProperty("站长评论署名（默认「博主」），并自动加入保留昵称")
    private String ownerNickname;

    @ApiModelProperty("保留昵称名单（逗号分隔），访客昵称包含其中任一词即拒绝")
    private String reservedNicknames;
}
