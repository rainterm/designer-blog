package com.designer.admin.module.blog.model.vo;

import lombok.Data;
import org.noear.snack4.annotation.ONodeAttr;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 公开评论 VO（支持楼中楼）
 *
 * @author 雨夜终端
 */
@Data
public class PublicCommentVO {

    /** 评论ID */
    private Long id;

    /** 父评论ID，0 为顶级 */
    private Long parentId;

    /** 评论人昵称 */
    private String nickname;

    /**
     * 作者类型：0 访客，1 博主。前端据此渲染「博主」标识，而不是看昵称文本。
     */
    private Integer authorType;

    /** 评论内容 */
    private String content;

    /** IP 归属地（省级，如「广东」） */
    private String ipLocation;

    /** 是否置顶：0 否，1 是。置顶的评论排在前面，前台展示为「置顶」标识 */
    private Integer isTop;

    /**
     * 被回复者的昵称：仅第 2 层有值，用于显示「某某 回复 某某」。
     * 被回复评论不可见时为 null。
     */
    private String replyToNickname;

    /** 评论时间 */
    @ONodeAttr(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 子回复列表（第 2 层，不再向下嵌套） */
    private List<PublicCommentVO> children;
}
