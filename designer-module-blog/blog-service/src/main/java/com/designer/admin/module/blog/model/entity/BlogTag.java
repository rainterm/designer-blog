package com.designer.admin.module.blog.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.designer.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 博客标签
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("blog_tag")
public class BlogTag extends BaseEntity {

    /** 标签名称（唯一） */
    private String name;

    /** 标签别名，用于前台 URL（唯一） */
    private String slug;

    /** 状态：1启用 0停用 */
    private Integer status;
}
