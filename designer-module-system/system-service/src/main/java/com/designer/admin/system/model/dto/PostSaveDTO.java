package com.designer.admin.system.model.dto;

import lombok.Data;
import org.noear.solon.validation.annotation.NotBlank;

/**
 * 岗位保存（新增/修改通用）
 */
@Data
public class PostSaveDTO {
    private Long id;

    @NotBlank(message = "岗位名称不能为空")
    private String postName;

    @NotBlank(message = "岗位编码不能为空")
    private String postCode;

    private Integer postLevel;
    private Integer sort;
    private Integer status;
    private String remark;
}
