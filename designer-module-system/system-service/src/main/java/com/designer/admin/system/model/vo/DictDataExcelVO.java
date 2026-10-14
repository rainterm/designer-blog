package com.designer.admin.system.model.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 字典数据导入导出 Excel VO
 */
@Data
public class DictDataExcelVO {

    @ExcelProperty("字典类型")
    private String dictType;

    @ExcelProperty("字典标签")
    private String dictLabel;

    @ExcelProperty("字典键值")
    private String dictValue;

    @ExcelProperty("排序")
    private Integer sort;

    @ExcelProperty("是否默认(1是0否)")
    private Integer isDefault;

    @ExcelProperty("状态(1正常0停用)")
    private Integer status;

    @ExcelProperty("备注")
    private String remark;
}
