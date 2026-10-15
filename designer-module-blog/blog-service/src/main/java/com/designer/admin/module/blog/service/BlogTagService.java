package com.designer.admin.module.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.module.blog.constant.BlogCacheTags;
import com.designer.admin.module.blog.mapper.BlogPostTagMapper;
import com.designer.admin.module.blog.mapper.BlogTagMapper;
import com.designer.admin.module.blog.model.dto.TagQueryDTO;
import com.designer.admin.module.blog.model.dto.TagSaveDTO;
import com.designer.admin.module.blog.model.entity.BlogTag;
import com.designer.admin.module.blog.util.SlugUtil;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.data.annotation.CacheRemove;

import java.util.List;

@Component
public class BlogTagService {

    @Db
    private BlogTagMapper tagMapper;
    @Db
    private BlogPostTagMapper postTagMapper;

    public IPage<BlogTag> page(TagQueryDTO query) {
        return tagMapper.selectPage(query.toPage(), new LambdaQueryWrapper<BlogTag>()
                .like(notBlank(query.getName()), BlogTag::getName, query.getName())
                .like(notBlank(query.getSlug()), BlogTag::getSlug, query.getSlug())
                .eq(query.getStatus() != null, BlogTag::getStatus, query.getStatus())
                .orderByDesc(BlogTag::getId));
    }

    /** 前台列表：仅启用（读缓存在 {@code BlogPublicController#tags}） */
    public List<BlogTag> listEnabled() {
        return tagMapper.selectList(new LambdaQueryWrapper<BlogTag>()
                .eq(BlogTag::getStatus, 1)
                .orderByAsc(BlogTag::getName));
    }

    public BlogTag getById(Long id) {
        BlogTag tag = tagMapper.selectById(id);
        if (tag == null) {
            throw new BizException("标签不存在");
        }
        return tag;
    }

    public BlogTag getBySlug(String slug) {
        return tagMapper.selectOne(new LambdaQueryWrapper<BlogTag>()
                .eq(BlogTag::getSlug, slug)
                .last("limit 1"));
    }

    /** 新增：失效前台标签列表缓存 */
    @CacheRemove(tags = BlogCacheTags.TAG)
    public void save(TagSaveDTO dto) {
        if (!notBlank(dto.getName())) {
            throw new BizException("标签名称不能为空");
        }
        assertNameUnique(dto.getName().trim(), null);
        String slug = ensureUniqueSlug(SlugUtil.resolve(dto.getSlug(), dto.getName()), null);
        BlogTag entity = toEntity(dto);
        entity.setSlug(slug);
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        tagMapper.insert(entity);
    }

    @CacheRemove(tags = BlogCacheTags.TAG)
    public void update(TagSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("标签ID不能为空");
        }
        getById(dto.getId());
        if (!notBlank(dto.getName())) {
            throw new BizException("标签名称不能为空");
        }
        assertNameUnique(dto.getName().trim(), dto.getId());
        String slug = ensureUniqueSlug(SlugUtil.resolve(dto.getSlug(), dto.getName()), dto.getId());
        BlogTag entity = toEntity(dto);
        entity.setSlug(slug);
        tagMapper.updateById(entity);
    }

    @CacheRemove(tags = BlogCacheTags.TAG)
    public void remove(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            postTagMapper.deleteByTagId(id);
        }
        tagMapper.deleteByIds(ids);
    }

    private void assertNameUnique(String name, Long excludeId) {
        Long cnt = tagMapper.selectCount(new LambdaQueryWrapper<BlogTag>()
                .eq(BlogTag::getName, name)
                .ne(excludeId != null, BlogTag::getId, excludeId));
        if (cnt != null && cnt > 0) {
            throw new BizException("标签名称已存在");
        }
    }

    private String ensureUniqueSlug(String base, Long excludeId) {
        String slug = base;
        int i = 0;
        while (true) {
            Long cnt = tagMapper.selectCount(new LambdaQueryWrapper<BlogTag>()
                    .eq(BlogTag::getSlug, slug)
                    .ne(excludeId != null, BlogTag::getId, excludeId));
            if (cnt == null || cnt == 0) {
                return slug;
            }
            i++;
            slug = base + "-" + i;
        }
    }

    private BlogTag toEntity(TagSaveDTO dto) {
        BlogTag e = new BlogTag();
        e.setId(dto.getId());
        e.setName(dto.getName().trim());
        e.setSlug(dto.getSlug());
        e.setStatus(dto.getStatus());
        return e;
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
