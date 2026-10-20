package com.designer.admin.module.blog.model.vo;

import com.designer.admin.common.core.DateTimePattern;
import lombok.Data;
import org.noear.snack4.annotation.ONodeAttr;

import java.time.LocalDateTime;

/**
 * 「我的评论」VO：评论人查看自己提交的评论（含未通过审核的）
 *
 * @author 雨夜终端
 */
@Data
public class MyCommentVO {

    /** 评论ID */
    private Long id;

    /** 所回复的评论 ID，0 为顶级 */
    private Long parentId;

    /** 评论内容 */
    private String content;

    /** 0待审 1通过 2拒绝 */
    private Integer status;

    /** 提交时间 */
    @ONodeAttr(format = DateTimePattern.DATE_TIME)
    private LocalDateTime createTime;
}
