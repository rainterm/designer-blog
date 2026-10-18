package com.designer.admin.module.blog.mapper;

import com.designer.admin.module.blog.model.entity.BlogTag;
import com.github.yulichang.base.MPJBaseMapper;
import com.github.yulichang.toolkit.JoinWrappers;

/**
 * 博客标签数据访问。
 *
 * @author 雨夜终端
 */
public interface BlogTagMapper extends MPJBaseMapper<BlogTag> {

    /**
     * 统计标签 slug 占用数（只统计未删除的标签）。
     *
     * <p>唯一性由应用层保证（表上无唯一索引），逻辑删除的标签不再占位。</p>
     *
     * @param slug      标签别名
     * @param excludeId 需要排除的标签 ID，新增时传 {@code null}
     * @return 占用该 slug 的记录数
     */
    default Long countBySlug(String slug, Long excludeId) {
        return selectJoinCount(JoinWrappers.lambda(BlogTag.class)
                .eq(BlogTag::getSlug, slug)
                .ne(excludeId != null, BlogTag::getId, excludeId));
    }

    /**
     * 统计标签名称占用数（只统计未删除的标签）。
     *
     * <p>唯一性由应用层保证（表上无唯一索引），逻辑删除的标签不再占位。</p>
     *
     * @param name      标签名称
     * @param excludeId 需要排除的标签 ID，新增时传 {@code null}
     * @return 占用该名称的记录数
     */
    default Long countByName(String name, Long excludeId) {
        return selectJoinCount(JoinWrappers.lambda(BlogTag.class)
                .eq(BlogTag::getName, name)
                .ne(excludeId != null, BlogTag::getId, excludeId));
    }
}
