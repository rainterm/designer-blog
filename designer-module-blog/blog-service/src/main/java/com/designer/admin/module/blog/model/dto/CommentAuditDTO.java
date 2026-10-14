package com.designer.admin.module.blog.model.dto;

import lombok.Data;

@Data
public class CommentAuditDTO {
    private Long id;
    /** 1通过 2拒绝 */
    private Integer status;
}
