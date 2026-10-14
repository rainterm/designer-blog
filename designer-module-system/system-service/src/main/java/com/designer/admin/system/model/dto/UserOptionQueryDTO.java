package com.designer.admin.system.model.dto;

import com.designer.admin.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 人员选项分页查询（账号/昵称模糊）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserOptionQueryDTO extends PageQuery {

    /** 账号或昵称关键字 */
    private String keyword;
}
