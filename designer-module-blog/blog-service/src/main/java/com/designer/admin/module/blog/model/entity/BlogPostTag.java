package com.designer.admin.module.blog.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 文章-标签关联
 *
 * @author 雨夜终端
 */
@Data
@TableName("blog_post_tag")
public class BlogPostTag {

    /** 文章ID */
    private Long postId;

    /** 标签ID */
    private Long tagId;
}
