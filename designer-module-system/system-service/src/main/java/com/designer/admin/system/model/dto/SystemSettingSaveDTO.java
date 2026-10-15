package com.designer.admin.system.model.dto;

import lombok.Data;
import org.noear.solon.validation.annotation.Length;
import org.noear.solon.validation.annotation.NotBlank;

/**
 * 系统设置保存
 */
@Data
public class SystemSettingSaveDTO {

    @NotBlank(message = "系统名称不能为空")
    @Length(max = 50, message = "系统名称长度不能超过 50 个字符")
    private String systemName;

    /** 登录页副标题，允许留空（留空则不显示） */
    @Length(max = 100, message = "登录页副标题长度不能超过 100 个字符")
    private String loginSubtitle;

    /** Logo 地址，允许留空（留空使用内置 Logo） */
    @Length(max = 500, message = "Logo 地址长度不能超过 500 个字符")
    private String logoUrl;

    /** Logo 圆角（px，0 为直角） */
    private Integer logoRadius;

    /** 默认布局模式 */
    private String defaultLayoutMode;

    /** 默认主题色（十六进制，如 #16A34A） */
    private String defaultPrimaryColor;
}
