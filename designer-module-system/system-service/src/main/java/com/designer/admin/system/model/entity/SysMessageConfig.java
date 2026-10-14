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
 * 消息通道配置（数据库驱动，可扩展 email / inbox / sms / dingtalk / ...）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_message_config")
public class SysMessageConfig extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String configName;

    /** 通道类型：email / inbox / sms / dingtalk / wechat / feishu */
    private String channelType;

    /** 同通道类型下是否默认 */
    private Integer isDefault;

    private Integer status;

    private String configJson;

    private String remark;

    @TableLogic
    @ONodeAttr(ignore = true)
    private Integer deleted;
}
