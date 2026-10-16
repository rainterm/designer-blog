package com.designer.admin.module.blog.model.vo;

import lombok.Data;
import org.noear.snack4.annotation.ONodeAttr;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 后台评论 VO：与 {@link PublicCommentVO} 同为**两层树**结构，额外带上审核与来源信息。
 *
 * <p>为什么不直接复用 {@code PublicCommentVO}：后台要展示 {@code status}（待审 / 通过 / 拒绝）
 * 与 {@code ip}，把它们加到公开 VO 上会一并出现在前台接口的响应里，等于泄露访客 IP。
 * 两个 VO 都只由 {@code BlogComment} 映射而来，字段重复的代价远小于这个泄露风险。</p>
 *
 * <p>{@code children} 刻意声明为 {@code AdminCommentVO} 而不是父类型，
 * 避免序列化时按父类型剔除子节点额外的 {@code status} 字段。</p>
 */
@Data
public class AdminCommentVO {

    private Long id;

    /** 0 为顶级 */
    private Long parentId;

    private String nickname;

    /** 作者类型：0 访客，1 博主。展示层的「博主」标识只看它，不看昵称文本 */
    private Integer authorType;

    private String content;

    private String ip;

    /** IP 归属地（省级，如「广东」） */
    private String ipLocation;

    /** 0待审 1通过 2拒绝 */
    private Integer status;

    /** 是否置顶：0 否，1 是。置顶的评论排在前面，展示为「置顶」标识 */
    private Integer isTop;

    /**
     * 被回复者的昵称：仅第 2 层有值，用于显示「回复 某某」。
     * 被回复评论不在本次结果里时为 null。
     */
    private String replyToNickname;

    @ONodeAttr(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    private List<AdminCommentVO> children = new ArrayList<>();
}
