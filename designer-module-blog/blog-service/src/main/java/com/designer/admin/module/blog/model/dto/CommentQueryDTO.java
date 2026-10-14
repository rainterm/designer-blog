package com.designer.admin.module.blog.model.dto;

import com.designer.admin.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CommentQueryDTO extends PageQuery {
    private Long postId;
    private String nickname;
    private Integer status;
    private String keyword;
}
