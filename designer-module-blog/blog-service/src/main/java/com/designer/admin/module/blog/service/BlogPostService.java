package com.designer.admin.module.blog.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.framework.satoken.LoginHelper;
import com.designer.admin.framework.satoken.LoginUser;
import com.designer.admin.module.blog.mapper.BlogCategoryMapper;
import com.designer.admin.module.blog.mapper.BlogCommentMapper;
import com.designer.admin.module.blog.mapper.BlogPostMapper;
import com.designer.admin.module.blog.mapper.BlogPostTagMapper;
import com.designer.admin.module.blog.mapper.BlogTagMapper;
import com.designer.admin.module.blog.model.dto.PostQueryDTO;
import com.designer.admin.module.blog.model.dto.PostSaveDTO;
import com.designer.admin.module.blog.model.entity.BlogCategory;
import com.designer.admin.module.blog.model.entity.BlogComment;
import com.designer.admin.module.blog.model.entity.BlogPost;
import com.designer.admin.module.blog.model.entity.BlogPostTag;
import com.designer.admin.module.blog.model.entity.BlogTag;
import com.designer.admin.module.blog.model.vo.PublicPostVO;
import com.designer.admin.module.blog.util.SlugUtil;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class BlogPostService {

    @Db
    private BlogPostMapper postMapper;
    @Db
    private BlogPostTagMapper postTagMapper;
    @Db
    private BlogTagMapper tagMapper;
    @Db
    private BlogCategoryMapper categoryMapper;
    @Db
    private BlogCommentMapper commentMapper;

    public IPage<BlogPost> page(PostQueryDTO query) {
        List<Long> postIdsByTag = null;
        if (query.getTagId() != null) {
            postIdsByTag = postTagMapper.selectPostIdsByTagId(query.getTagId());
            if (postIdsByTag == null || postIdsByTag.isEmpty()) {
                Page<BlogPost> empty = query.toPage();
                empty.setRecords(Collections.emptyList());
                empty.setTotal(0);
                return empty;
            }
        }
        String keyword = firstNonBlank(query.getKeyword(), query.getTitle());
        IPage<BlogPost> page = postMapper.selectPage(query.toPage(), new LambdaQueryWrapper<BlogPost>()
                .and(notBlank(keyword), w -> w
                        .like(BlogPost::getTitle, keyword)
                        .or()
                        .like(BlogPost::getSummary, keyword))
                .eq(query.getCategoryId() != null, BlogPost::getCategoryId, query.getCategoryId())
                .eq(query.getStatus() != null, BlogPost::getStatus, query.getStatus())
                .eq(query.getIsTop() != null, BlogPost::getIsTop, query.getIsTop())
                .in(postIdsByTag != null, BlogPost::getId, postIdsByTag)
                .orderByDesc(BlogPost::getIsTop)
                .orderByDesc(BlogPost::getPublishedAt)
                .orderByDesc(BlogPost::getId));
        fillExtras(page.getRecords());
        return page;
    }

    public BlogPost getById(Long id) {
        BlogPost post = postMapper.selectById(id);
        if (post == null) {
            throw new BizException("文章不存在");
        }
        fillExtras(List.of(post));
        return post;
    }

    public void save(PostSaveDTO dto) {
        validateSave(dto);
        BlogPost entity = toEntity(dto);
        entity.setSlug(ensureUniqueSlug(SlugUtil.resolve(dto.getSlug(), dto.getTitle()), null));
        applyAuthor(entity);
        applyPublishFields(entity, null);
        if (entity.getViewCount() == null) {
            entity.setViewCount(0);
        }
        postMapper.insert(entity);
        replaceTags(entity.getId(), dto.getTagIds());
    }

    public void update(PostSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("文章ID不能为空");
        }
        BlogPost exist = getById(dto.getId());
        validateSave(dto);
        BlogPost entity = toEntity(dto);
        entity.setSlug(ensureUniqueSlug(SlugUtil.resolve(dto.getSlug(), dto.getTitle()), dto.getId()));
        entity.setAuthorId(exist.getAuthorId());
        entity.setAuthorName(exist.getAuthorName());
        entity.setViewCount(exist.getViewCount());
        applyPublishFields(entity, exist);
        postMapper.updateById(entity);
        replaceTags(entity.getId(), dto.getTagIds());
    }

    public void publish(Long id, boolean publish) {
        BlogPost exist = getById(id);
        BlogPost update = new BlogPost();
        update.setId(id);
        update.setStatus(publish ? 1 : 0);
        if (publish) {
            update.setPublishedAt(exist.getPublishedAt() == null ? LocalDateTime.now() : exist.getPublishedAt());
        }
        postMapper.updateById(update);
    }

    public void remove(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            postTagMapper.deleteByPostId(id);
            commentMapper.delete(new LambdaQueryWrapper<BlogComment>().eq(BlogComment::getPostId, id));
        }
        postMapper.deleteByIds(ids);
    }

    /** 公开：已发布文章分页 */
    public IPage<PublicPostVO> publicPage(PostQueryDTO query) {
        query.setStatus(1);
        Long categoryId = query.getCategoryId();
        Long tagId = query.getTagId();
        List<Long> postIdsByTag = null;
        if (tagId != null) {
            postIdsByTag = postTagMapper.selectPostIdsByTagId(tagId);
            if (postIdsByTag == null || postIdsByTag.isEmpty()) {
                Page<PublicPostVO> empty = new Page<>(query.getPageNum(), query.getPageSize());
                empty.setRecords(Collections.emptyList());
                empty.setTotal(0);
                return empty;
            }
        }
        String keyword = firstNonBlank(query.getKeyword(), query.getTitle());
        IPage<BlogPost> page = postMapper.selectPage(query.toPage(), new LambdaQueryWrapper<BlogPost>()
                .eq(BlogPost::getStatus, 1)
                .and(notBlank(keyword), w -> w
                        .like(BlogPost::getTitle, keyword)
                        .or()
                        .like(BlogPost::getSummary, keyword))
                .eq(categoryId != null, BlogPost::getCategoryId, categoryId)
                .in(postIdsByTag != null, BlogPost::getId, postIdsByTag)
                .orderByDesc(BlogPost::getIsTop)
                .orderByDesc(BlogPost::getPublishedAt)
                .orderByDesc(BlogPost::getId));
        fillExtras(page.getRecords());
        Page<PublicPostVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(p -> toPublicVO(p, false)).collect(Collectors.toList()));
        return voPage;
    }

    public PublicPostVO publicDetailBySlug(String slug) {
        BlogPost post = postMapper.selectOne(new LambdaQueryWrapper<BlogPost>()
                .eq(BlogPost::getSlug, slug)
                .eq(BlogPost::getStatus, 1)
                .last("limit 1"));
        if (post == null) {
            throw new BizException("文章不存在或未发布");
        }
        postMapper.update(null, new LambdaUpdateWrapper<BlogPost>()
                .eq(BlogPost::getId, post.getId())
                .setSql("view_count = view_count + 1"));
        post.setViewCount(post.getViewCount() == null ? 1 : post.getViewCount() + 1);
        fillExtras(List.of(post));
        return toPublicVO(post, true);
    }

    private void validateSave(PostSaveDTO dto) {
        if (!notBlank(dto.getTitle())) {
            throw new BizException("标题不能为空");
        }
        if (!notBlank(dto.getContent())) {
            throw new BizException("正文不能为空");
        }
    }

    private void applyAuthor(BlogPost entity) {
        entity.setAuthorId(LoginHelper.getUserId());
        Object obj = StpUtil.getSession().get(LoginHelper.LOGIN_USER_KEY);
        if (obj instanceof LoginUser lu) {
            entity.setAuthorName(notBlank(lu.getNickname()) ? lu.getNickname() : lu.getUsername());
        } else {
            entity.setAuthorName(LoginHelper.getUsername());
        }
    }

    private void applyPublishFields(BlogPost entity, BlogPost exist) {
        if (entity.getStatus() == null) {
            entity.setStatus(0);
        }
        if (entity.getIsTop() == null) {
            entity.setIsTop(0);
        }
        if (entity.getAllowComment() == null) {
            entity.setAllowComment(1);
        }
        if (entity.getCoverUrl() == null) {
            entity.setCoverUrl("");
        }
        if (entity.getSummary() == null) {
            entity.setSummary("");
        }
        if (entity.getSeoTitle() == null) {
            entity.setSeoTitle("");
        }
        if (entity.getSeoKeywords() == null) {
            entity.setSeoKeywords("");
        }
        if (entity.getSeoDescription() == null) {
            entity.setSeoDescription("");
        }
        if (Objects.equals(entity.getStatus(), 1)) {
            if (exist == null || exist.getPublishedAt() == null) {
                entity.setPublishedAt(LocalDateTime.now());
            } else {
                entity.setPublishedAt(exist.getPublishedAt());
            }
        } else if (exist != null) {
            entity.setPublishedAt(exist.getPublishedAt());
        }
    }

    private void replaceTags(Long postId, List<Long> tagIds) {
        postTagMapper.deleteByPostId(postId);
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        for (Long tagId : tagIds) {
            if (tagId == null) {
                continue;
            }
            BlogPostTag rel = new BlogPostTag();
            rel.setPostId(postId);
            rel.setTagId(tagId);
            postTagMapper.insert(rel);
        }
    }

    private void fillExtras(List<BlogPost> posts) {
        if (posts == null || posts.isEmpty()) {
            return;
        }
        List<Long> categoryIds = posts.stream()
                .map(BlogPost::getCategoryId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, BlogCategory> categoryMap = new HashMap<>();
        if (!categoryIds.isEmpty()) {
            List<BlogCategory> cats = categoryMapper.selectByIds(categoryIds);
            for (BlogCategory c : cats) {
                categoryMap.put(c.getId(), c);
            }
        }
        for (BlogPost post : posts) {
            if (post.getCategoryId() != null) {
                BlogCategory c = categoryMap.get(post.getCategoryId());
                if (c != null) {
                    post.setCategoryName(c.getName());
                }
            }
            List<Long> tagIds = postTagMapper.selectTagIdsByPostId(post.getId());
            post.setTagIds(tagIds == null ? Collections.emptyList() : tagIds);
            if (tagIds != null && !tagIds.isEmpty()) {
                List<BlogTag> tags = tagMapper.selectByIds(tagIds);
                post.setTagNames(tags.stream().map(BlogTag::getName).collect(Collectors.toList()));
            } else {
                post.setTagNames(Collections.emptyList());
            }
        }
    }

    private PublicPostVO toPublicVO(BlogPost post, boolean withContent) {
        PublicPostVO vo = new PublicPostVO();
        vo.setId(post.getId());
        vo.setTitle(post.getTitle());
        vo.setSlug(post.getSlug());
        vo.setSummary(post.getSummary());
        vo.setContent(withContent ? post.getContent() : null);
        vo.setCoverUrl(post.getCoverUrl());
        vo.setCategoryId(post.getCategoryId());
        vo.setCategoryName(post.getCategoryName());
        if (post.getCategoryId() != null) {
            BlogCategory c = categoryMapper.selectById(post.getCategoryId());
            if (c != null) {
                vo.setCategorySlug(c.getSlug());
                vo.setCategoryName(c.getName());
            }
        }
        vo.setAuthorName(post.getAuthorName());
        vo.setIsTop(post.getIsTop());
        vo.setAllowComment(post.getAllowComment());
        vo.setViewCount(post.getViewCount());
        vo.setSeoTitle(post.getSeoTitle());
        vo.setSeoKeywords(post.getSeoKeywords());
        vo.setSeoDescription(post.getSeoDescription());
        vo.setPublishedAt(post.getPublishedAt());
        List<Long> tagIds = post.getTagIds();
        if (tagIds == null) {
            tagIds = postTagMapper.selectTagIdsByPostId(post.getId());
        }
        if (tagIds != null && !tagIds.isEmpty()) {
            List<BlogTag> tags = tagMapper.selectByIds(tagIds);
            vo.setTagNames(tags.stream().map(BlogTag::getName).collect(Collectors.toList()));
            vo.setTagSlugs(tags.stream().map(BlogTag::getSlug).collect(Collectors.toList()));
        } else {
            vo.setTagNames(Collections.emptyList());
            vo.setTagSlugs(Collections.emptyList());
        }
        return vo;
    }

    private String ensureUniqueSlug(String base, Long excludeId) {
        String slug = base;
        int i = 0;
        while (true) {
            Long cnt = postMapper.selectCount(new LambdaQueryWrapper<BlogPost>()
                    .eq(BlogPost::getSlug, slug)
                    .ne(excludeId != null, BlogPost::getId, excludeId));
            if (cnt == null || cnt == 0) {
                return slug;
            }
            i++;
            slug = base + "-" + i;
        }
    }

    private BlogPost toEntity(PostSaveDTO dto) {
        BlogPost e = new BlogPost();
        e.setId(dto.getId());
        e.setTitle(dto.getTitle().trim());
        e.setSlug(dto.getSlug());
        e.setSummary(dto.getSummary());
        e.setContent(dto.getContent());
        e.setCoverUrl(dto.getCoverUrl());
        e.setCategoryId(dto.getCategoryId());
        e.setStatus(dto.getStatus());
        e.setIsTop(dto.getIsTop());
        e.setAllowComment(dto.getAllowComment());
        e.setSeoTitle(dto.getSeoTitle());
        e.setSeoKeywords(dto.getSeoKeywords());
        e.setSeoDescription(dto.getSeoDescription());
        return e;
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private String firstNonBlank(String a, String b) {
        if (notBlank(a)) {
            return a;
        }
        if (notBlank(b)) {
            return b;
        }
        return null;
    }
}
