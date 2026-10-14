package com.designer.admin.module.blog.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.designer.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 博客链接（社交/外链，如 GitHub、Gitee）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("blog_link")
public class BlogLink extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String url;

    /** 图标标识（前端按此选图标，如 github/gitee/link） */
    private String icon;

    private Integer sort;

    /** 1启用 0停用 */
    private Integer status;

    private String remark;
}
