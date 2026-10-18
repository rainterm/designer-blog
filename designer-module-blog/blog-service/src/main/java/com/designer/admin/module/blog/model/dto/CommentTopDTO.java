package com.designer.admin.module.blog.model.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 后台置顶 / 取消置顶评论。
 *
 * <p>用显式的 {@code isTop} 而不是「切换」语义：重复提交同一个请求结果一致，
 * 前端列表刷新后也不会因为重放而反复翻转。</p>
 *
 * @author 雨夜终端
 */
@Data
public class CommentTopDTO {

    @ApiModelProperty(value = "评论 ID", required = true)
    private Long id;

    @ApiModelProperty(value = "是否置顶：0否 1是", required = true)
    private Integer isTop;
}
