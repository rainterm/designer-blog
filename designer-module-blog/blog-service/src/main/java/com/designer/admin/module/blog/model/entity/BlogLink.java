package com.designer.admin.module.blog.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.designer.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 博客链接（社交/外链，如 GitHub、Gitee）
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("blog_link")
public class BlogLink extends BaseEntity {

    /** 链接名称 */
    private String name;

    /** 跳转地址 */
    private String url;

    /** 图标标识（前端按此选图标，如 github/gitee/link） */
    private String icon;

    /** 显示顺序，越小越靠前 */
    private Integer sort;

    /** 状态：1启用 0停用 */
    private Integer status;

    /** 备注 */
    private String remark;
}
