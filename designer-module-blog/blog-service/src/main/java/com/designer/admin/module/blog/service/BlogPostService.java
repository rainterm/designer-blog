package com.designer.admin.module.blog.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.module.blog.model.dto.PostQueryDTO;
import com.designer.admin.module.blog.model.dto.PostSaveDTO;
import com.designer.admin.module.blog.model.entity.BlogPost;
import com.designer.admin.module.blog.model.vo.PublicPostVO;

import java.util.List;

/**
 * 博客文章：后台 CRUD / 发布 / 标签关联，前台分页与详情。
 *
 * @author 雨夜终端
 */
public interface BlogPostService {

    /**
     * 后台文章分页，支持标题 / 分类 / 标签 / 状态筛选，结果补充标签等信息。
     *
     * @param query 分页与筛选条件
     * @return 文章分页结果
     */
    IPage<BlogPost> listPage(PostQueryDTO query);

    /**
     * 按主键查询文章（含标签等扩展信息），不存在时抛出业务异常。
     *
     * @param id 文章 ID
     * @return 文章
     */
    BlogPost getById(Long id);

    /**
     * 新增文章，保存标签关联，并按状态补齐发布时间。
     *
     * @param dto 保存参数
     */
    void add(PostSaveDTO dto);

    /**
     * 修改文章并同步标签关联；作者与阅读数保持不变。
     *
     * @param dto 保存参数
     */
    void update(PostSaveDTO dto);

    /**
     * 发布 / 下架文章。
     *
     * @param id      文章 ID
     * @param publish true=发布，false=下架
     */
    void publish(Long id, boolean publish);

    /**
     * 批量删除文章及标签关联。
     *
     * @param ids 文章 ID 列表
     */
    void remove(List<Long> ids);

    /**
     * 前台文章分页：仅已发布，支持分类 / 标签 / 关键词筛选，按置顶与发布时间倒序。
     *
     * @param query 分页与筛选条件
     * @return 文章分页结果
     */
    IPage<PublicPostVO> listPublicPage(PostQueryDTO query);

    /**
     * 前台按 slug 查询已发布文章详情（含标签与阅读数增量）。
     *
     * @param slug 文章别名
     * @return 文章详情
     */
    PublicPostVO getPublicDetailBySlug(String slug);
}
