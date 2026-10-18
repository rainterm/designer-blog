package com.designer.admin.module.blog.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.designer.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 博客分类
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("blog_category")
public class BlogCategory extends BaseEntity {

    /** 分类名称 */
    private String name;

    /** 分类别名，用于前台 URL（唯一） */
    private String slug;

    /** 显示顺序，越小越靠前 */
    private Integer sort;

    /** 状态：1启用 0停用 */
    private Integer status;

    /** 备注 */
    private String remark;
}
