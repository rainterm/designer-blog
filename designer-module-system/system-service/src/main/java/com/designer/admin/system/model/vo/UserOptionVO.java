package com.designer.admin.system.model.vo;

import lombok.Data;

/**
 * 人员选择弹框/下拉用：仅暴露选人所需字段，不含手机、邮箱、角色等敏感信息。
 */
@Data
public class UserOptionVO {

    private Long id;
    private String username;
    private String nickname;
    private String deptName;
}
