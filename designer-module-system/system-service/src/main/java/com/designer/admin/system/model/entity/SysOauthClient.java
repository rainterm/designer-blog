package com.designer.admin.system.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.designer.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.noear.snack4.annotation.ONodeAttr;

import java.util.List;

/**
 * OAuth2 客户端（Client Credentials）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_oauth_client")
public class SysOauthClient extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String clientId;

    @ONodeAttr(ignore = true)
    private String clientSecret;

    private String clientName;
    private Integer accessTokenTtl;
    private Integer status;
    private String remark;

    @TableField(exist = false)
    private List<Long> scopeIds;

    @TableField(exist = false)
    private List<String> scopeCodes;

    @TableLogic
    @ONodeAttr(ignore = true)
    private Integer deleted;
}
