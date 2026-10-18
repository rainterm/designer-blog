package com.designer.admin.module.blog.mapper;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.designer.admin.module.blog.model.entity.BlogPost;
import com.github.yulichang.base.MPJBaseMapper;
import com.github.yulichang.toolkit.JoinWrappers;

/**
 * 博客文章数据访问。
 *
 * @author 雨夜终端
 */
public interface BlogPostMapper extends MPJBaseMapper<BlogPost> {

    /**
     * 阅读数增量回写（由 {@code BlogViewFlushJob} 定时调用）。
     *
     * <p>语义是「原子加一个数」：交给数据库完成自增，不先读后写，也就没有并发覆盖问题。
     * 用 {@code setSql} 的 {@code {0}} 参数占位而不是字符串拼接 {@code + delta}，
     * 避免后续改动把外部输入拼进 SQL；参数由 MyBatis 绑定，走的是预编译占位符。</p>
     *
     * @param id    文章 ID
     * @param delta 增量（正整数）
     * @return 受影响行数
     */
    default int incrementViewCount(Long id, long delta) {
        return update(null, new LambdaUpdateWrapper<BlogPost>()
                .setSql("view_count = view_count + {0}", delta)
                .eq(BlogPost::getId, id));
    }

    /**
     * 统计文章 slug 占用数（只统计未删除的文章）。
     *
     * <p>唯一性由应用层保证（表上无唯一索引），逻辑删除的文章不再占位。</p>
     *
     * @param slug      文章别名
     * @param excludeId 需要排除的文章 ID，新增时传 {@code null}
     * @return 占用该 slug 的记录数
     */
    default Long countBySlug(String slug, Long excludeId) {
        return selectJoinCount(JoinWrappers.lambda(BlogPost.class)
                .eq(BlogPost::getSlug, slug)
                .ne(excludeId != null, BlogPost::getId, excludeId));
    }
}
