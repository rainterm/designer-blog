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
}
