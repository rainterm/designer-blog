package com.designer.admin.module.blog.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.yulichang.base.MPJBaseMapper;
import com.designer.admin.module.blog.model.entity.BlogPostTag;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public interface BlogPostTagMapper extends MPJBaseMapper<BlogPostTag> {

    default int deleteByPostId(Long postId) {
        return delete(new LambdaQueryWrapper<BlogPostTag>().eq(BlogPostTag::getPostId, postId));
    }

    default int deleteByTagId(Long tagId) {
        return delete(new LambdaQueryWrapper<BlogPostTag>().eq(BlogPostTag::getTagId, tagId));
    }

    default List<Long> selectTagIdsByPostId(Long postId) {
        List<BlogPostTag> list = selectList(new LambdaQueryWrapper<BlogPostTag>()
                .eq(BlogPostTag::getPostId, postId));
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        return list.stream().map(BlogPostTag::getTagId).collect(Collectors.toList());
    }

    default List<Long> selectPostIdsByTagId(Long tagId) {
        List<BlogPostTag> list = selectList(new LambdaQueryWrapper<BlogPostTag>()
                .eq(BlogPostTag::getTagId, tagId));
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        return list.stream().map(BlogPostTag::getPostId).collect(Collectors.toList());
    }
}
