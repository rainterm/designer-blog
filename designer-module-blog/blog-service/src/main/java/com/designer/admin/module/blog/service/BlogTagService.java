package com.designer.admin.module.blog.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.module.blog.model.dto.TagQueryDTO;
import com.designer.admin.module.blog.model.dto.TagSaveDTO;
import com.designer.admin.module.blog.model.entity.BlogTag;

import java.util.List;

/**
 * 博客标签：CRUD、slug 唯一化与前台启用列表。
 *
 * @author 雨夜终端
 */
public interface BlogTagService {

    /**
     * 标签分页。
     *
     * @param query 分页与名称 / slug / 状态筛选条件
     * @return 标签分页结果
     */
    IPage<BlogTag> listPage(TagQueryDTO query);

    /**
     * 前台列表：仅启用（读缓存在 {@code BlogPublicController#tags}）。
     *
     * @return 标签列表
     */
    List<BlogTag> listEnabled();

    /**
     * 按主键查询标签，不存在时抛出业务异常。
     *
     * @param id 标签 ID
     * @return 标签
     */
    BlogTag getById(Long id);

    /**
     * 按 slug 查询标签，用于前台路由。
     *
     * @param slug 标签别名
     * @return 标签；不存在时返回 null
     */
    BlogTag getBySlug(String slug);

    /**
     * 新增标签，slug 冲突时自动追加序号；同时失效前台标签缓存。
     *
     * @param dto 保存参数
     */
    void add(TagSaveDTO dto);

    /**
     * 修改标签，slug 冲突时自动追加序号；同时失效前台标签缓存。
     *
     * @param dto 保存参数
     */
    void update(TagSaveDTO dto);

    /**
     * 批量删除标签，并清理文章标签关联；同时失效前台标签缓存。
     *
     * @param ids 标签 ID 列表
     */
    void remove(List<Long> ids);
}
