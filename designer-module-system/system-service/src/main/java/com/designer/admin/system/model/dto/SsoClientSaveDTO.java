package com.designer.admin.system.model.dto;

import lombok.Data;
import org.noear.solon.validation.annotation.NotBlank;

/**
 * SSO 客户端保存（新增/修改通用）
 */
@Data
public class SsoClientSaveDTO {
    private Long id;

    @NotBlank(message = "客户端名称不能为空")
    private String clientName;

    @NotBlank(message = "客户端标识不能为空")
    private String clientKey;

    @NotBlank(message = "允许回跳 Origin 不能为空")
    private String origin;

    @NotBlank(message = "打开地址不能为空")
    private String openUrl;

    private String entryPath;
    private Integer sort;
    private Integer status;
    private String remark;
}
