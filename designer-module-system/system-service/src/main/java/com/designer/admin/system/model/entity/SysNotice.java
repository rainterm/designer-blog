package com.designer.admin.system.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.designer.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 站内消息（消息通道 inbox 投递；用户在顶部消息中心接收）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_notice")
public class SysNotice extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 状态：1正常 0关闭 */
    private Integer status;

    /** 接收人登录账号，空=全体用户；多个逗号分隔 */
    private String receivers;

    /** 备注 */
    private String remark;

    /** 当前用户是否已读（非库字段） */
    @TableField(exist = false)
    private Boolean readFlag;

    /** 前端跳转路径（非库字段，由 remark 解析） */
    @TableField(exist = false)
    private String link;
}
