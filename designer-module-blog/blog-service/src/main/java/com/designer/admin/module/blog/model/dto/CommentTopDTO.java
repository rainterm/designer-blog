package com.designer.admin.module.blog.model.dto;

import lombok.Data;

/**
 * 后台置顶 / 取消置顶评论。
 *
 * <p>用显式的 {@code isTop} 而不是「切换」语义：重复提交同一个请求结果一致，
 * 前端列表刷新后也不会因为重放而反复翻转。</p>
 */
@Data
public class CommentTopDTO {

    /** 评论ID */
    private Long id;

    /** 是否置顶：0 否，1 是 */
    private Integer isTop;
}
