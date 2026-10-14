package com.designer.admin.system.model.dto;

import lombok.Data;
import org.noear.solon.validation.annotation.NotBlank;

@Data
public class OauthScopeSaveDTO {
    private Long id;

    @NotBlank(message = "Scope 标识不能为空")
    private String scopeCode;

    @NotBlank(message = "Scope 名称不能为空")
    private String scopeName;

    @NotBlank(message = "路径模式不能为空")
    private String pathPattern;

    private Integer sort;
    private Integer status;
    private String remark;
}
