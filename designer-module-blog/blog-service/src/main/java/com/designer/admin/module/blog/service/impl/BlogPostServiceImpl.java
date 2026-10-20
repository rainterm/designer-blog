package com.designer.admin.module.blog.service.impl;

import com.designer.admin.module.blog.constant.BlogPostConsts;
import com.designer.admin.common.constant.SystemConsts;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.designer.admin.common.core.ResultCode;
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
import com.designer.admin.module.blog.service.BlogPostService;
import com.designer.admin.module.blog.service.BlogViewCounter;
import com.designer.admin.module.blog.util.SlugUtil;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 文章服务实现。
 *
 * @author 雨夜终端
 */
@Component
public class BlogPostServiceImpl implements BlogPostService {

    /**
     * 「更新于」提示的阈值。
     *
     * <p>发布时间与 {@code update_time} 是两次 {@code now()} 调用，落库到秒，
     * 同一秒落库时差值为 0；留 1 分钟余量，跨秒的那 1 秒不会被当成「编辑过」。</p>
     */
    private static final Duration UPDATE_HINT_THRESHOLD = Duration.ofMinutes(1);

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
    @Inject
    private BlogViewCounter viewCounter;

    @Override
    public IPage<BlogPost> listPage(PostQueryDTO query) {
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
        IPage<BlogPost> resultPage = postMapper.selectPage(query.toPage(), new LambdaQueryWrapper<BlogPost>()
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
        fillExtras(resultPage.getRecords());
        return resultPage;
    }

    @Override
    public BlogPost getById(Long id) {
        BlogPost post = postMapper.selectById(id);
        if (post == null) {
            throw new BizException("文章不存在");
        }
        fillExtras(List.of(post));
        return post;
    }

    @Override
    public void add(PostSaveDTO dto) {
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

    @Override
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

    @Override
    public void publish(Long id, boolean publish) {
        BlogPost exist = getById(id);
        BlogPost update = new BlogPost();
        update.setId(id);
        update.setStatus(publish ? SystemConsts.STATUS_ENABLE : SystemConsts.STATUS_DISABLE);
        if (publish) {
            update.setPublishedAt(exist.getPublishedAt() == null ? LocalDateTime.now() : exist.getPublishedAt());
        }
        postMapper.updateById(update);
    }

    @Override
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

    @Override
    public IPage<PublicPostVO> listPublicPage(PostQueryDTO query) {
        query.setStatus(SystemConsts.STATUS_ENABLE);
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
        IPage<BlogPost> resultPage = postMapper.selectPage(query.toPage(), new LambdaQueryWrapper<BlogPost>()
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
        fillExtras(resultPage.getRecords());
        applyPendingViews(resultPage.getRecords());
        Page<PublicPostVO> voPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        voPage.setRecords(resultPage.getRecords().stream().map(p -> toPublicVO(p, false)).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public PublicPostVO getPublicDetailBySlug(String slug) {
        BlogPost post = postMapper.selectOne(new LambdaQueryWrapper<BlogPost>()
                .eq(BlogPost::getSlug, slug)
                .eq(BlogPost::getStatus, 1)
                .last("limit 1"));
        if (post == null) {
            throw new BizException(ResultCode.NOT_FOUND, "文章不存在或未发布");
        }
        fillExtras(List.of(post));
        applyPendingViews(List.of(post));
        return toPublicVO(post, true);
    }

    /**
     * 把 Redis 里尚未回写的阅读增量加到展示值上。
     *
     * <p>阅读数的真实值 = 「库里已落地的 + 还没回写的」。缺了后半截，刚发布的文章
     * 会明显偏少（一条都还没回写过），也会出现「刷新一下数字跳一截」的观感。
     * 列表与详情都走这里，保证两处显示一致。</p>
     *
     * <p>整批只发一次 {@code HMGET}；Redis 不可用时内部已降级为 0，不会影响出页。</p>
     */
    private void applyPendingViews(List<BlogPost> posts) {
        if (posts == null || posts.isEmpty()) {
            return;
        }
        Map<Long, Long> pending = viewCounter.pendingBatch(
                posts.stream().map(BlogPost::getId).collect(Collectors.toList()));
        if (pending.isEmpty()) {
            return;
        }
        for (BlogPost post : posts) {
            Long delta = pending.get(post.getId());
            if (delta == null) {
                continue;
            }
            int stored = post.getViewCount() == null ? 0 : post.getViewCount();
            post.setViewCount((int) (stored + delta));
        }
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
            entity.setStatus(SystemConsts.STATUS_DISABLE);
        }
        if (entity.getIsTop() == null) {
            entity.setIsTop(BlogPostConsts.TOP_FLAG_OFF);
        }
        if (entity.getAllowComment() == null) {
            entity.setAllowComment(BlogPostConsts.COMMENT_ENABLED);
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
        if (Objects.equals(entity.getStatus(), SystemConsts.STATUS_ENABLE)) {
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
        Map<Long, BlogCategory> categoryMap = HashMap.newHashMap(categoryIds.size());
        if (!categoryIds.isEmpty()) {
            List<BlogCategory> cats = categoryMapper.selectByIds(categoryIds);
            for (BlogCategory c : cats) {
                categoryMap.put(c.getId(), c);
            }
        }
        // 标签必须批量查：逐篇查的话一页 5 篇就是 10 条 SQL
        //（每篇一次 selectTagIdsByPostId + 一次 selectByIds），会吃掉列表页的绝大部分查询。
        // 批量后无论多少篇都只有 2 条（关联表 1 条 + 标签表 1 条）。
        List<Long> postIds = posts.stream()
                .map(BlogPost::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, List<Long>> tagIdsByPost = postTagMapper.selectTagIdsByPostIds(postIds);
        List<Long> allTagIds = tagIdsByPost.values().stream()
                .flatMap(List::stream)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, BlogTag> tagMap = HashMap.newHashMap(allTagIds.size());
        if (!allTagIds.isEmpty()) {
            for (BlogTag t : tagMapper.selectByIds(allTagIds)) {
                tagMap.put(t.getId(), t);
            }
        }
        for (BlogPost post : posts) {
            if (post.getCategoryId() != null) {
                BlogCategory c = categoryMap.get(post.getCategoryId());
                if (c != null) {
                    // slug 一并填上，避免 toPublicVO 为了拿它再查一次分类
                    post.setCategoryName(c.getName());
                    post.setCategorySlug(c.getSlug());
                }
            }
            List<Long> tagIds = tagIdsByPost.getOrDefault(post.getId(), Collections.emptyList());
            post.setTagIds(tagIds);
            // 名称与 slug 都在这里定下来，toPublicVO 只做字段搬运
            List<BlogTag> tags = tagIds.stream()
                    .map(tagMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            post.setTagNames(tags.stream().map(BlogTag::getName).collect(Collectors.toList()));
            post.setTagSlugs(tags.stream().map(BlogTag::getSlug).collect(Collectors.toList()));
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
        // 分类名与 slug 由 fillExtras 批量填好，这里只做搬运。
        // ⚠ 不要在这里再 selectById 查分类：fillExtras 刚查过同一批，
        // 列表页上会变成「每篇多一条」的重复查询
        vo.setCategorySlug(post.getCategorySlug());
        vo.setAuthorName(post.getAuthorName());
        vo.setIsTop(post.getIsTop());
        vo.setAllowComment(post.getAllowComment());
        vo.setViewCount(post.getViewCount());
        vo.setSeoTitle(post.getSeoTitle());
        vo.setSeoKeywords(post.getSeoKeywords());
        vo.setSeoDescription(post.getSeoDescription());
        vo.setPublishedAt(post.getPublishedAt());
        // 编辑时间与发布时间分开：前台按 updated 决定要不要提示「更新于」，
        // 判断逻辑留在服务端，前端不做任何时间比较
        vo.setUpdateTime(post.getUpdateTime());
        vo.setUpdated(shouldShowUpdate(post.getPublishedAt(), post.getUpdateTime()));
        // 标签同理：名称与 slug 都在 fillExtras 里一次性填好，这里只做搬运。
        // ⚠ 不要在这里再查 tagMapper.selectByIds，与 fillExtras 完全重复
        vo.setTagNames(post.getTagNames() == null ? Collections.emptyList() : post.getTagNames());
        vo.setTagSlugs(post.getTagSlugs() == null ? Collections.emptyList() : post.getTagSlugs());
        return vo;
    }

    /**
     * 是否值得向前台提示「更新于」。
     *
     * <p>阈值取 1 分钟而不是「严格大于」：发布与自动填充的 update_time 是两次
     * {@code now()} 调用，落库精度到秒，跨秒时会出现 1 秒的差值。用严格大于会把这种
     * 「刚发布就提示已更新」的噪音放到线上。</p>
     *
     * <p>未发布（publishedAt 为空）但已有 update_time（草稿被编辑过）时返回 false：
     * 草稿不在前台展示，这条分支只在未来开放草稿预览时才有意义，先保守处理。</p>
     */
    private static boolean shouldShowUpdate(LocalDateTime publishedAt, LocalDateTime updateTime) {
        if (publishedAt == null || updateTime == null) {
            return false;
        }
        return updateTime.isAfter(publishedAt.plus(UPDATE_HINT_THRESHOLD));
    }

    private String ensureUniqueSlug(String base, Long excludeId) {
        String slug = base;
        int i = 0;
        while (true) {
            Long cnt = postMapper.countBySlug(slug, excludeId);
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
