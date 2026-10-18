package com.designer.admin.module.blog.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.module.blog.model.dto.CategoryQueryDTO;
import com.designer.admin.module.blog.model.dto.CategorySaveDTO;
import com.designer.admin.module.blog.model.entity.BlogCategory;

import java.util.List;

/**
 * 博客分类：CRUD、slug 唯一化与前台启用列表。
 *
 * @author 雨夜终端
 */
public interface BlogCategoryService {

    /**
     * 分类分页。
     *
     * @param query 分页与名称 / slug / 状态筛选条件
     * @return 分类分页结果
     */
    IPage<BlogCategory> listPage(CategoryQueryDTO query);

    /**
     * 前台列表：仅启用，按排序升序（读缓存在 {@code BlogPublicController#categories}）。
     *
     * @return 分类列表
     */
    List<BlogCategory> listEnabled();

    /**
     * 按主键查询分类，不存在时抛出业务异常。
     *
     * @param id 分类 ID
     * @return 分类
     */
    BlogCategory getById(Long id);

    /**
     * 按 slug 查询分类，用于前台路由。
     *
     * @param slug 分类别名
     * @return 分类；不存在时返回 null
     */
    BlogCategory getBySlug(String slug);

    /**
     * 新增分类，slug 冲突时自动追加序号；同时失效前台分类缓存。
     *
     * @param dto 保存参数
     */
    void add(CategorySaveDTO dto);

    /**
     * 修改分类，slug 冲突时自动追加序号；同时失效前台分类缓存。
     *
     * @param dto 保存参数
     */
    void update(CategorySaveDTO dto);

    /**
     * 批量删除分类；分类下仍有文章时拒绝。
     *
     * @param ids 分类 ID 列表
     */
    void remove(List<Long> ids);
}
