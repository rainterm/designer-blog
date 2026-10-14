package com.designer.admin.system.model.dto;

import lombok.Data;
import org.noear.solon.validation.annotation.NotBlank;

/**
 * 菜单保存（新增/修改通用）
 */
@Data
public class MenuSaveDTO {

    private Long id;
    private Long parentId;

    @NotBlank(message = "菜单名称不能为空")
    private String menuName;

    /** M目录 C菜单 F按钮 */
    @NotBlank(message = "菜单类型不能为空")
    private String menuType;

    private String path;
    private String component;
    private String perms;
    private String icon;
    private Integer sort;
    private Integer visible;
    private Integer status;
}
