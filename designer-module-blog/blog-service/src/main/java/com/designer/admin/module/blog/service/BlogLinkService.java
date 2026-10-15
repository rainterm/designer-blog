package com.designer.admin.module.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.module.blog.constant.BlogCacheTags;
import com.designer.admin.module.blog.mapper.BlogLinkMapper;
import com.designer.admin.module.blog.model.dto.LinkQueryDTO;
import com.designer.admin.module.blog.model.dto.LinkSaveDTO;
import com.designer.admin.module.blog.model.entity.BlogLink;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.data.annotation.CacheRemove;

import java.util.List;

/**
 * 博客链接（社交/外链）：后台维护 + 前台展示
 */
@Component
public class BlogLinkService {

    @Db
    private BlogLinkMapper linkMapper;

    public IPage<BlogLink> page(LinkQueryDTO query) {
        return linkMapper.selectPage(query.toPage(), new LambdaQueryWrapper<BlogLink>()
                .like(notBlank(query.getName()), BlogLink::getName, query.getName())
                .eq(query.getStatus() != null, BlogLink::getStatus, query.getStatus())
                .orderByAsc(BlogLink::getSort)
                .orderByAsc(BlogLink::getId));
    }

    /** 前台展示用：仅启用，按排序升序（读缓存在 {@code BlogPublicController#links}） */
    public List<BlogLink> listEnabled() {
        return linkMapper.selectList(new LambdaQueryWrapper<BlogLink>()
                .eq(BlogLink::getStatus, 1)
                .orderByAsc(BlogLink::getSort)
                .orderByAsc(BlogLink::getId));
    }

    public BlogLink getById(Long id) {
        BlogLink link = linkMapper.selectById(id);
        if (link == null) {
            throw new BizException("链接不存在");
        }
        return link;
    }

    /** 新增：失效前台外链缓存 */
    @CacheRemove(tags = BlogCacheTags.LINK)
    public void save(LinkSaveDTO dto) {
        validate(dto);
        BlogLink entity = toEntity(dto);
        if (entity.getSort() == null) {
            entity.setSort(0);
        }
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        linkMapper.insert(entity);
    }

    @CacheRemove(tags = BlogCacheTags.LINK)
    public void update(LinkSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("链接ID不能为空");
        }
        getById(dto.getId());
        validate(dto);
        linkMapper.updateById(toEntity(dto));
    }

    @CacheRemove(tags = BlogCacheTags.LINK)
    public void remove(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        linkMapper.deleteByIds(ids);
    }

    private void validate(LinkSaveDTO dto) {
        if (!notBlank(dto.getName())) {
            throw new BizException("链接名称不能为空");
        }
        String url = dto.getUrl() == null ? "" : dto.getUrl().trim();
        if (url.isEmpty()) {
            throw new BizException("跳转地址不能为空");
        }
        String lower = url.toLowerCase();
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            throw new BizException("跳转地址需以 http:// 或 https:// 开头");
        }
    }

    private BlogLink toEntity(LinkSaveDTO dto) {
        BlogLink e = new BlogLink();
        e.setId(dto.getId());
        e.setName(dto.getName().trim());
        e.setUrl(dto.getUrl().trim());
        e.setIcon(dto.getIcon() == null ? "" : dto.getIcon().trim());
        e.setSort(dto.getSort());
        e.setStatus(dto.getStatus());
        e.setRemark(dto.getRemark() == null ? "" : dto.getRemark());
        return e;
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
