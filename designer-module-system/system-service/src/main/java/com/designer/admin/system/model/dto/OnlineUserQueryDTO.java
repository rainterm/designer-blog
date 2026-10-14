package com.designer.admin.system.model.dto;

import com.designer.admin.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 在线用户查询
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OnlineUserQueryDTO extends PageQuery {
    /** 登录账号（模糊） */
    private String username;

    /** 登录 IP（模糊） */
    private String ip;
}
