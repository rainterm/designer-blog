package com.designer.admin.system.model.dto;

import lombok.Data;
import org.noear.solon.validation.annotation.NotBlank;

import java.util.List;

@Data
public class OauthClientSaveDTO {
    private Long id;

    @NotBlank(message = "client_id 不能为空")
    private String clientId;

    @NotBlank(message = "客户端名称不能为空")
    private String clientName;

    private Integer accessTokenTtl;
    private Integer status;
    private String remark;
    private List<Long> scopeIds;
}
