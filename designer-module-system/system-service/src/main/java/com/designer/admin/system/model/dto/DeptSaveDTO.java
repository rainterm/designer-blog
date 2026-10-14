package com.designer.admin.system.model.dto;

import lombok.Data;
import org.noear.solon.validation.annotation.NotBlank;

/**
 * 部门保存
 */
@Data
public class DeptSaveDTO {

    private Long id;

    /** 父部门，空/null 视为 0 */
    private Long parentId;

    @NotBlank(message = "部门名称不能为空")
    private String deptName;

    private Integer sort;
    private Integer status;
}
