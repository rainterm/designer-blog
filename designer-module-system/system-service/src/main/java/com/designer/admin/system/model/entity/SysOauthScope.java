package com.designer.admin.system.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.designer.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.noear.snack4.annotation.ONodeAttr;

/**
 * OAuth2 开放接口 Scope
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_oauth_scope")
public class SysOauthScope extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String scopeCode;
    private String scopeName;
    private String pathPattern;
    private Integer sort;
    private Integer status;
    private String remark;

    @TableLogic
    @ONodeAttr(ignore = true)
    private Integer deleted;
}
