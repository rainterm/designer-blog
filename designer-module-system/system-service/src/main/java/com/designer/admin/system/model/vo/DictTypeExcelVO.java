package com.designer.admin.system.model.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 字典类型导入导出 Excel VO
 */
@Data
public class DictTypeExcelVO {

    @ExcelProperty("字典名称")
    private String dictName;

    @ExcelProperty("字典类型")
    private String dictType;

    @ExcelProperty("状态(1正常0停用)")
    private Integer status;

    @ExcelProperty("备注")
    private String remark;
}
