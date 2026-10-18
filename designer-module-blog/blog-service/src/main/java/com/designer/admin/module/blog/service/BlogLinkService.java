package com.designer.admin.module.blog.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.module.blog.model.dto.LinkQueryDTO;
import com.designer.admin.module.blog.model.dto.LinkSaveDTO;
import com.designer.admin.module.blog.model.entity.BlogLink;

import java.util.List;

/**
 * 博客链接（社交/外链）：后台维护 + 前台展示
 *
 * @author 雨夜终端
 */
public interface BlogLinkService {

    /**
     * 外链分页。
     *
     * @param query 分页与名称 / 状态筛选条件
     * @return 外链分页结果
     */
    IPage<BlogLink> listPage(LinkQueryDTO query);

    /**
     * 前台外链列表：仅启用，按排序升序（读缓存在 {@code BlogPublicController#links}）。
     *
     * @return 外链列表
     */
    List<BlogLink> listEnabled();

    /**
     * 按主键查询外链，不存在时抛出业务异常。
     *
     * @param id 外链 ID
     * @return 外链
     */
    BlogLink getById(Long id);

    /**
     * 新增外链；同时失效前台外链缓存。
     *
     * @param dto 保存参数
     */
    void add(LinkSaveDTO dto);

    /**
     * 修改外链；同时失效前台外链缓存。
     *
     * @param dto 保存参数
     */
    void update(LinkSaveDTO dto);

    /**
     * 批量删除外链；同时失效前台外链缓存。
     *
     * @param ids 外链 ID 列表
     */
    void remove(List<Long> ids);
}
