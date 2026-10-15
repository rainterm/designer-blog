package com.designer.admin.module.blog.model.dto;

import lombok.Data;

@Data
public class SettingSaveDTO {
    private String siteName;
    private String siteSubtitle;
    private String logoUrl;
    private Integer logoRadius;
    private String copyright;
    private String icp;
    private String icpUrl;
    private String seoKeywords;
    private String seoDescription;
    private Integer commentEnabled;

    /** 站长评论署名（默认「博主」）：后台回复的默认署名，并自动加入保留昵称 */
    private String ownerNickname;

    /** 保留昵称名单（逗号分隔）：访客昵称包含其中任一词即拒绝 */
    private String reservedNicknames;
}
