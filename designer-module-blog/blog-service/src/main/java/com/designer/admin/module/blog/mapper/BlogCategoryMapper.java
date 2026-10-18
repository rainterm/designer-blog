package com.designer.admin.module.blog.mapper;

import com.designer.admin.module.blog.model.entity.BlogCategory;
import com.github.yulichang.base.MPJBaseMapper;
import com.github.yulichang.toolkit.JoinWrappers;

/**
 * 博客分类数据访问。
 *
 * @author 雨夜终端
 */
public interface BlogCategoryMapper extends MPJBaseMapper<BlogCategory> {

    /**
     * 统计分类 slug 占用数（只统计未删除的分类）。
     *
     * <p>唯一性由应用层保证（表上无唯一索引），逻辑删除的分类不再占位，
     * 所以删除后同一个 slug 可以重新使用。</p>
     *
     * @param slug      分类别名
     * @param excludeId 需要排除的分类 ID，新增时传 {@code null}
     * @return 占用该 slug 的记录数
     */
    default Long countBySlug(String slug, Long excludeId) {
        return selectJoinCount(JoinWrappers.lambda(BlogCategory.class)
                .eq(BlogCategory::getSlug, slug)
                .ne(excludeId != null, BlogCategory::getId, excludeId));
    }
}
