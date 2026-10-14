package com.designer.admin.system.model.dto;

import lombok.Data;

/**
 * 字典类型保存（新增/修改通用）
 */
@Data
public class DictTypeSaveDTO {
    private Long id;
    private String dictName;
    private String dictType;
    private Integer status;
    private String remark;
}
