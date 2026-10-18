package com.designer.admin.module.blog.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.yulichang.base.MPJBaseMapper;
import com.designer.admin.module.blog.model.entity.BlogPostTag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 文章与标签的关联数据访问。
 *
 * @author 雨夜终端
 */
public interface BlogPostTagMapper extends MPJBaseMapper<BlogPostTag> {

    /**
     * 删除某篇文章的全部标签关联。
     *
     * @param postId 文章 ID
     * @return 删除行数
     */
    default int deleteByPostId(Long postId) {
        return delete(new LambdaQueryWrapper<BlogPostTag>().eq(BlogPostTag::getPostId, postId));
    }

    /**
     * 删除某个标签的全部文章关联。
     *
     * @param tagId 标签 ID
     * @return 删除行数
     */
    default int deleteByTagId(Long tagId) {
        return delete(new LambdaQueryWrapper<BlogPostTag>().eq(BlogPostTag::getTagId, tagId));
    }

    /**
     * 单篇文章的标签 ID。
     *
     * <p>⚠ 已有调用方只需一个 postId 时也优先考虑 {@link #selectTagIdsByPostIds}：
     * 传单元素列表同样只发一条 SQL，而循环调用本方法就是 N+1。
     * 保留它只是为了「确实只查一篇」的零散场景。</p>
     *
     * @param postId 文章 ID
     * @return 标签 ID 列表，没有标签时返回空列表
     */
    default List<Long> selectTagIdsByPostId(Long postId) {
        List<BlogPostTag> list = selectList(new LambdaQueryWrapper<BlogPostTag>()
                .eq(BlogPostTag::getPostId, postId));
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        return list.stream().map(BlogPostTag::getTagId).collect(Collectors.toList());
    }

    /**
     * 批量查多篇文章的标签 ID：{@code postId -> tagIds}，没有标签的文章不出现在 Map 里。
     *
     * <p>存在的意义只有一个：消灭 N+1。列表页每篇都调一次
     * {@link #selectTagIdsByPostId} 的话，一页 5 篇就是 5 条 SQL，
     * 而这里合成 1 条。返回 Map 而不是 List 是为了让调用方直接按 postId 取，
     * 不必自己再分组。</p>
     *
     * @param postIds 文章 ID 列表
     * @return 文章 ID 到标签 ID 列表的映射；没有标签的文章不会出现在 Map 中
     */
    default Map<Long, List<Long>> selectTagIdsByPostIds(List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<BlogPostTag> list = selectList(new LambdaQueryWrapper<BlogPostTag>()
                .in(BlogPostTag::getPostId, postIds));
        if (list == null || list.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, List<Long>> grouped = HashMap.newHashMap(list.size());
        for (BlogPostTag rel : list) {
            grouped.computeIfAbsent(rel.getPostId(), k -> new ArrayList<>()).add(rel.getTagId());
        }
        return grouped;
    }

    /**
     * 查询某个标签下的全部文章 ID。
     *
     * @param tagId 标签 ID
     * @return 文章 ID 列表，没有文章时返回空列表
     */
    default List<Long> selectPostIdsByTagId(Long tagId) {
        List<BlogPostTag> list = selectList(new LambdaQueryWrapper<BlogPostTag>()
                .eq(BlogPostTag::getTagId, tagId));
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        return list.stream().map(BlogPostTag::getPostId).collect(Collectors.toList());
    }
}
