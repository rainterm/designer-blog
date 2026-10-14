package com.designer.admin.system.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.noear.snack4.annotation.ONodeAttr;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知公告已读记录
 */
@Data
@TableName("sys_notice_read")
public class SysNoticeRead implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long noticeId;

    private Long userId;

    @ONodeAttr(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime readTime;
}
