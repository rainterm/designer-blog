package com.designer.admin.module.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.module.blog.mapper.BlogCategoryMapper;
import com.designer.admin.module.blog.mapper.BlogPostMapper;
import com.designer.admin.module.blog.model.dto.CategoryQueryDTO;
import com.designer.admin.module.blog.model.dto.CategorySaveDTO;
import com.designer.admin.module.blog.model.entity.BlogCategory;
import com.designer.admin.module.blog.model.entity.BlogPost;
import com.designer.admin.module.blog.util.SlugUtil;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;

import java.util.List;

@Component
public class BlogCategoryService {

    @Db
    private BlogCategoryMapper categoryMapper;
    @Db
    private BlogPostMapper postMapper;

    public IPage<BlogCategory> page(CategoryQueryDTO query) {
        return categoryMapper.selectPage(query.toPage(), new LambdaQueryWrapper<BlogCategory>()
                .like(notBlank(query.getName()), BlogCategory::getName, query.getName())
                .like(notBlank(query.getSlug()), BlogCategory::getSlug, query.getSlug())
                .eq(query.getStatus() != null, BlogCategory::getStatus, query.getStatus())
                .orderByAsc(BlogCategory::getSort)
                .orderByAsc(BlogCategory::getId));
    }

    public List<BlogCategory> listEnabled() {
        return categoryMapper.selectList(new LambdaQueryWrapper<BlogCategory>()
                .eq(BlogCategory::getStatus, 1)
                .orderByAsc(BlogCategory::getSort)
                .orderByAsc(BlogCategory::getId));
    }

    public BlogCategory getById(Long id) {
        BlogCategory cat = categoryMapper.selectById(id);
        if (cat == null) {
            throw new BizException("分类不存在");
        }
        return cat;
    }

    public BlogCategory getBySlug(String slug) {
        return categoryMapper.selectOne(new LambdaQueryWrapper<BlogCategory>()
                .eq(BlogCategory::getSlug, slug)
                .last("limit 1"));
    }

    public void save(CategorySaveDTO dto) {
        if (!notBlank(dto.getName())) {
            throw new BizException("分类名称不能为空");
        }
        String slug = ensureUniqueSlug(SlugUtil.resolve(dto.getSlug(), dto.getName()), null);
        BlogCategory entity = toEntity(dto);
        entity.setSlug(slug);
        if (entity.getSort() == null) {
            entity.setSort(0);
        }
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        if (entity.getRemark() == null) {
            entity.setRemark("");
        }
        categoryMapper.insert(entity);
    }

    public void update(CategorySaveDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("分类ID不能为空");
        }
        getById(dto.getId());
        if (!notBlank(dto.getName())) {
            throw new BizException("分类名称不能为空");
        }
        String slug = ensureUniqueSlug(SlugUtil.resolve(dto.getSlug(), dto.getName()), dto.getId());
        BlogCategory entity = toEntity(dto);
        entity.setSlug(slug);
        categoryMapper.updateById(entity);
    }

    public void remove(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        Long cnt = postMapper.selectCount(new LambdaQueryWrapper<BlogPost>()
                .in(BlogPost::getCategoryId, ids));
        if (cnt != null && cnt > 0) {
            throw new BizException("分类下仍有文章，无法删除");
        }
        categoryMapper.deleteByIds(ids);
    }

    private String ensureUniqueSlug(String base, Long excludeId) {
        String slug = base;
        int i = 0;
        while (true) {
            Long cnt = categoryMapper.selectCount(new LambdaQueryWrapper<BlogCategory>()
                    .eq(BlogCategory::getSlug, slug)
                    .ne(excludeId != null, BlogCategory::getId, excludeId));
            if (cnt == null || cnt == 0) {
                return slug;
            }
            i++;
            slug = base + "-" + i;
        }
    }

    private BlogCategory toEntity(CategorySaveDTO dto) {
        BlogCategory e = new BlogCategory();
        e.setId(dto.getId());
        e.setName(dto.getName().trim());
        e.setSlug(dto.getSlug());
        e.setSort(dto.getSort());
        e.setStatus(dto.getStatus());
        e.setRemark(dto.getRemark() == null ? "" : dto.getRemark());
        return e;
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
