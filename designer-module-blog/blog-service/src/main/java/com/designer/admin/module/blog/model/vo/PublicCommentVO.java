package com.designer.admin.module.blog.model.vo;

import lombok.Data;
import org.noear.snack4.annotation.ONodeAttr;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 公开评论 VO（支持楼中楼）
 */
@Data
public class PublicCommentVO {
    private Long id;
    private Long parentId;
    private String nickname;
    private String content;

    /** IP 归属地（省级，如「广东」） */
    private String ipLocation;

    /**
     * 被回复者的昵称：仅第 2 层有值，用于显示「某某 回复 某某」。
     * 被回复评论不可见时为 null。
     */
    private String replyToNickname;

    @ONodeAttr(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    private List<PublicCommentVO> children = new ArrayList<>();
}
