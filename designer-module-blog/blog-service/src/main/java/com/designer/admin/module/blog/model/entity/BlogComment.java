package com.designer.admin.module.blog.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.designer.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 博客评论
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("blog_comment")
public class BlogComment extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long postId;

    /** 0 为顶级 */
    private Long parentId;

    private String nickname;

    /**
     * 作者类型：0 访客，1 博主。
     *
     * <p>这是**身份的唯一权威来源**：展示层的「博主」标识只看这个字段，
     * 绝不根据昵称文本判断，否则访客把昵称写成「博主」即可冒充。
     * 只有后台回复（{@code BlogCommentService#reply}）会写入 1，
     * 访客提交一律强制为 0（不取前端传值）。</p>
     */
    private Integer authorType;

    private String email;

    private String content;

    private String ip;

    /** IP 归属地（省级，如「广东」；内网为「内网」；无法判定为「未知」） */
    private String ipLocation;

    /** 评论人匿名标识：客户端密钥的 SHA-256，用于「我的评论」查看与删除 */
    private String commenterKey;

    /** 0待审 1通过 2拒绝 */
    private Integer status;

    /** 文章标题（非库字段） */
    @TableField(exist = false)
    private String postTitle;
}
