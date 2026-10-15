package com.designer.admin.module.blog.mapper;

import com.designer.admin.module.blog.model.entity.BlogPost;
import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface BlogPostMapper extends MPJBaseMapper<BlogPost> {

    /**
     * 阅读数增量回写（由 {@code BlogViewFlushJob} 定时调用）。
     *
     * <p>刻意用参数占位而不是 {@code LambdaUpdateWrapper.setSql("view_count = view_count + " + delta)}：
     * 字符串拼接的写法在后续改动里很容易被塞进外部输入，而这里的语义是「原子加一个数」，
     * 用 {@code #{delta}} 既安全又能让数据库自己完成自增（不需要先读后写，也就没有并发覆盖问题）。</p>
     *
     * @param id    文章 ID
     * @param delta 增量（正整数）
     * @return 受影响行数
     */
    @Update("update blog_post set view_count = view_count + #{delta} where id = #{id}")
    int incrementViewCount(@Param("id") Long id, @Param("delta") long delta);
}
