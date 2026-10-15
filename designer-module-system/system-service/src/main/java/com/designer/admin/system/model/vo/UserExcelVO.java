package com.designer.admin.system.model.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 用户导入导出 Excel VO
 */
@Data
public class UserExcelVO {

    @ExcelProperty("登录账号")
    private String username;

    @ExcelProperty("昵称")
    private String nickname;

    @ExcelProperty("邮箱")
    private String email;

    @ExcelProperty("手机号")
    private String phone;

    @ExcelProperty("部门ID")
    private Long deptId;

    @ExcelProperty("状态(1正常0停用)")
    private Integer status;

    @ExcelProperty("备注")
    private String remark;

    /** 仅导入使用；导出为空。**导入时必填**，不提供默认密码 */
    @ExcelProperty("初始密码")
    private String password;
}
