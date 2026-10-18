package com.designer.admin.module.blog.service.impl;

import com.designer.admin.module.blog.constant.BlogPostConsts;
import com.designer.admin.common.constant.SystemConsts;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.common.core.PageQuery;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.framework.captcha.CaptchaVerifier;
import com.designer.admin.framework.client.ClientIpUtil;
import com.designer.admin.framework.client.IpRegionResolver;
import com.designer.admin.module.blog.mapper.BlogCommentMapper;
import com.designer.admin.module.blog.constant.BlogCommentConsts;
import com.designer.admin.module.blog.mapper.BlogPostMapper;
import com.designer.admin.module.blog.model.dto.CommentAddDTO;
import com.designer.admin.module.blog.model.dto.CommentAuditDTO;
import com.designer.admin.module.blog.model.dto.CommentQueryDTO;
import com.designer.admin.module.blog.model.dto.CommentReplyDTO;
import com.designer.admin.module.blog.model.dto.CommentTopDTO;
import com.designer.admin.module.blog.model.dto.CommentUpdateDTO;
import com.designer.admin.module.blog.model.dto.PublicCommentSaveDTO;
import com.designer.admin.module.blog.model.entity.BlogComment;
import com.designer.admin.module.blog.model.entity.BlogPost;
import com.designer.admin.module.blog.model.vo.AdminCommentVO;
import com.designer.admin.module.blog.model.vo.MyCommentVO;
import com.designer.admin.module.blog.model.vo.PublicCommentVO;
import com.designer.admin.module.blog.service.BlogCommentService;
import com.designer.admin.module.blog.service.BlogSettingService;
import com.designer.admin.module.blog.util.CommenterKeyUtil;
import com.designer.admin.module.blog.util.NicknameUtil;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 评论服务实现。
 *
 * @author 雨夜终端
 */
@Component
public class BlogCommentServiceImpl implements BlogCommentService {

    /** 宽松的邮箱格式校验：只要「本地部分@域名.后缀」即可，不追求 RFC 完整 */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    @Db
    private BlogCommentMapper commentMapper;
    @Db
    private BlogPostMapper postMapper;
    @Inject
    private CaptchaVerifier captchaVerifier;
    @Inject
    private BlogSettingService settingService;
    @Inject
    private IpRegionResolver ipRegionResolver;

    @Override
    public IPage<BlogComment> listPage(CommentQueryDTO query) {
        IPage<BlogComment> resultPage = commentMapper.selectPage(query.toPage(), new LambdaQueryWrapper<BlogComment>()
                .eq(query.getPostId() != null, BlogComment::getPostId, query.getPostId())
                .like(notBlank(query.getNickname()), BlogComment::getNickname, query.getNickname())
                .eq(query.getStatus() != null, BlogComment::getStatus, query.getStatus())
                .and(notBlank(query.getKeyword()), w -> w
                        .like(BlogComment::getContent, query.getKeyword())
                        .or()
                        .like(BlogComment::getNickname, query.getKeyword()))
                .orderByDesc(BlogComment::getId));
        fillPostTitle(resultPage.getRecords());
        return resultPage;
    }

    @Override
    public long countPending() {
        Long count = commentMapper.selectCount(new LambdaQueryWrapper<BlogComment>()
                .eq(BlogComment::getStatus, 0));
        return count == null ? 0L : count;
    }

    @Override
    public void audit(CommentAuditDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("评论ID不能为空");
        }
        if (!isValidAuditStatus(dto.getStatus())) {
            throw new BizException("审核状态无效");
        }
        BlogComment exist = commentMapper.selectById(dto.getId());
        if (exist == null) {
            throw new BizException("评论不存在");
        }
        BlogComment update = new BlogComment();
        update.setId(dto.getId());
        update.setStatus(dto.getStatus());
        commentMapper.updateById(update);
    }

    @Override
    public void remove(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        commentMapper.deleteByIds(ids);
    }

    @Override
    public void reply(CommentReplyDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("评论ID不能为空");
        }
        String content = requireContent(dto.getContent(), "回复");
        BlogComment parent = commentMapper.selectById(dto.getId());
        if (parent == null) {
            throw new BizException("评论不存在");
        }
        // 只允许回复已通过的评论：回复会以 status=1 立即公开，若父评论仍在待审/已拒绝，
        // 公开列表会因父评论被过滤而把这条回复「孤儿提升」成顶级评论，
        // 读者会看到一句没有上下文的博主发言。先审核再回复，行为才可预期。
        if (!Objects.equals(parent.getStatus(), BlogCommentConsts.STATUS_APPROVED)) {
            throw new BizException("只能回复已通过的评论，请先审核通过");
        }

        BlogComment reply = new BlogComment();
        reply.setPostId(parent.getPostId());
        reply.setParentId(parent.getId());
        /*
         * 署名固定取站点配置的站长署名，不接受调用方自定义：
         * 回复入口再给一个昵称输入框，就会出现「后台回复」署名各自不同的情况，
         * 与 author_type=1 这个统一身份标记相矛盾，也容易和访客昵称混淆。
         * 要改署名请去「站点设置 → 站长署名」，所有回复一起生效。
         */
        reply.setNickname(settingService.getOwnerNickname());
        reply.setAuthorType(BlogCommentConsts.AUTHOR_TYPE_OWNER);
        reply.setEmail("");
        reply.setContent(content);
        reply.setIp("");
        reply.setIpLocation("");
        // 管理员回复不携带访客密钥，避免被识别为某个浏览器的「我的评论」
        reply.setCommenterKey("");
        reply.setStatus(BlogCommentConsts.STATUS_APPROVED);
        commentMapper.insert(reply);
    }

    @Override
    public void addByOwner(CommentAddDTO dto) {
        if (dto.getPostId() == null) {
            throw new BizException("文章ID不能为空");
        }
        String content = requireContent(dto.getContent(), "评论");

        BlogPost post = postMapper.selectById(dto.getPostId());
        if (post == null || !Objects.equals(post.getStatus(), SystemConsts.STATUS_ENABLE)) {
            throw new BizException("文章不存在或未发布");
        }

        BlogComment comment = new BlogComment();
        comment.setPostId(dto.getPostId());
        comment.setParentId(BlogCommentConsts.ROOT_PARENT_ID);
        comment.setNickname(settingService.getOwnerNickname());
        comment.setAuthorType(BlogCommentConsts.AUTHOR_TYPE_OWNER);
        comment.setEmail("");
        comment.setContent(content);
        // 与 reply 同理：代表站点方发言，不记 IP / 归属地，也不带访客密钥
        comment.setIp("");
        comment.setIpLocation("");
        comment.setCommenterKey("");
        // 站长自己的发言无需再走审核，直接公开
        comment.setStatus(BlogCommentConsts.STATUS_APPROVED);
        commentMapper.insert(comment);
    }

    @Override
    public void updateContent(CommentUpdateDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("评论ID不能为空");
        }
        String content = requireContent(dto.getContent(), "评论");
        BlogComment exist = commentMapper.selectById(dto.getId());
        if (exist == null) {
            throw new BizException("评论不存在");
        }

        BlogComment update = new BlogComment();
        update.setId(dto.getId());
        update.setContent(content);
        commentMapper.updateById(update);
    }

    @Override
    public void updateTop(CommentTopDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("评论ID不能为空");
        }
        if (!isValidTopFlag(dto.getIsTop())) {
            throw new BizException("置顶标记无效");
        }
        BlogComment exist = commentMapper.selectById(dto.getId());
        if (exist == null) {
            throw new BizException("评论不存在");
        }
        BlogComment update = new BlogComment();
        update.setId(dto.getId());
        update.setIsTop(dto.getIsTop());
        commentMapper.updateById(update);
    }

    /**
     * 正文校验：非空 + 长度上限（与 {@code blog_comment.content} 列宽一致）。
     *
     * <p>{@code label} 用于拼提示语，让「回复」与「评论」两类入口的报错各自贴合上下文。</p>
     */
    private String requireContent(String raw, String label) {
        if (!notBlank(raw)) {
            throw new BizException(label + "内容不能为空");
        }
        String content = raw.trim();
        if (content.length() > BlogCommentConsts.MAX_CONTENT_LENGTH) {
            throw new BizException(label + "内容过长");
        }
        return content;
    }

    @Override
    public List<PublicCommentVO> listApprovedTree(Long postId) {
        List<BlogComment> list = commentMapper.selectList(new LambdaQueryWrapper<BlogComment>()
                .eq(BlogComment::getPostId, postId)
                .eq(BlogComment::getStatus, 1)
                .orderByAsc(BlogComment::getId));
        Map<Long, String> nicknames = nicknameIndex(list);
        List<PublicCommentVO> roots = new ArrayList<>();
        for (List<BlogComment> members : groupTwoLevels(list).values()) {
            // 组内按 id 升序，且顶级评论先于其回复创建，所以第一条就是顶级
            PublicCommentVO root = toPublicVO(members.get(0));
            for (int i = 1; i < members.size(); i++) {
                BlogComment child = members.get(i);
                PublicCommentVO vo = toPublicVO(child);
                vo.setReplyToNickname(replyToName(child.getParentId(), nicknames));
                root.getChildren().add(vo);
            }
            roots.add(root);
        }
        return roots;
    }

    @Override
    public PageResult<AdminCommentVO> listAdminTreePage(Long postId, PageQuery query) {
        if (postId == null) {
            throw new BizException("文章ID不能为空");
        }
        List<BlogComment> list = commentMapper.selectList(new LambdaQueryWrapper<BlogComment>()
                .eq(BlogComment::getPostId, postId)
                .orderByAsc(BlogComment::getId));
        Map<Long, String> nicknames = nicknameIndex(list);
        List<AdminCommentVO> roots = new ArrayList<>();
        for (List<BlogComment> members : groupTwoLevels(list).values()) {
            AdminCommentVO root = toAdminVO(members.get(0));
            for (int i = 1; i < members.size(); i++) {
                BlogComment child = members.get(i);
                AdminCommentVO vo = toAdminVO(child);
                vo.setReplyToNickname(replyToName(child.getParentId(), nicknames));
                root.getChildren().add(vo);
            }
            roots.add(root);
        }

        long total = roots.size();
        long pageNum = query.getPageNum();
        long pageSize = query.getPageSize();
        int from = (int) Math.min(total, (pageNum - 1) * pageSize);
        int to = (int) Math.min(total, (long) from + pageSize);
        return new PageResult<>(total, pageNum, pageSize,
                from >= total ? new ArrayList<>() : new ArrayList<>(roots.subList(from, to)));
    }

    /**
     * 把评论压平成「两层」分组：顶级评论 id -> 该组全部成员（组长在前，其余按展示顺序）。
     *
     * <p>公开树与后台树共用这一份分组，让「后台看到的层级与顺序」与「前台看到的」
     * 由同一套规则决定，而不是各写一份、日后慢慢走样。</p>
     */
    private Map<Long, List<BlogComment>> groupTwoLevels(List<BlogComment> list) {
        Map<Long, BlogComment> byId = LinkedHashMap.newLinkedHashMap(list.size());
        for (BlogComment c : list) {
            byId.put(c.getId(), c);
        }
        Map<Long, List<BlogComment>> grouped = LinkedHashMap.newLinkedHashMap(list.size());
        Map<Long, Long> rootIdCache = HashMap.newHashMap(list.size());
        for (BlogComment c : list) {
            grouped.computeIfAbsent(resolveRootId(c, byId, rootIdCache), k -> new ArrayList<>()).add(c);
        }

        // 顶级之间的先后由置顶决定，所以不能沿用上面的插入顺序，需重新排一次
        List<Map.Entry<Long, List<BlogComment>>> entries = new ArrayList<>(grouped.entrySet());
        entries.sort(Comparator.comparing(
                (Map.Entry<Long, List<BlogComment>> e) -> e.getValue().get(0), DISPLAY_ORDER));

        Map<Long, List<BlogComment>> ordered = new LinkedHashMap<>();
        for (Map.Entry<Long, List<BlogComment>> e : entries) {
            List<BlogComment> members = e.getValue();
            // 组内第一条恒为顶级评论（顶级先于其回复创建，入组又按 id 升序）。
            // 只排回复：若连同顶级一起排，被置顶的回复会跑到自己的顶级评论前面。
            List<BlogComment> replies = new ArrayList<>(members.subList(1, members.size()));
            replies.sort(DISPLAY_ORDER);
            List<BlogComment> row = new ArrayList<>(members.size());
            row.add(members.get(0));
            row.addAll(replies);
            ordered.put(e.getKey(), row);
        }
        return ordered;
    }

    /**
     * 评论展示顺序：置顶优先，其余按 id 升序。
     *
     * <p>次序键用 id 而不是「置顶时间」：后者要额外加一列，换来的只是
     * 「同时置顶多条时最后置顶的排最前」。置顶条数通常极少，这个差异可以接受，
     * 而按 id 排序结果稳定、可预测。</p>
     */
    private static final Comparator<BlogComment> DISPLAY_ORDER =
            Comparator.comparingInt((BlogComment c) -> isTop(c) ? 0 : 1)
                    .thenComparing(BlogComment::getId);

    private static boolean isTop(BlogComment c) {
        return c.getIsTop() != null && c.getIsTop() == 1;
    }

    private PublicCommentVO toPublicVO(BlogComment c) {
        PublicCommentVO vo = new PublicCommentVO();
        vo.setId(c.getId());
        vo.setParentId(c.getParentId());
        vo.setNickname(c.getNickname());
        vo.setAuthorType(authorTypeOf(c));
        vo.setContent(c.getContent());
        vo.setIpLocation(c.getIpLocation());
        vo.setIsTop(topFlagOf(c));
        vo.setCreateTime(c.getCreateTime());
        return vo;
    }

    private AdminCommentVO toAdminVO(BlogComment c) {
        AdminCommentVO vo = new AdminCommentVO();
        vo.setId(c.getId());
        vo.setParentId(c.getParentId());
        vo.setNickname(c.getNickname());
        vo.setAuthorType(authorTypeOf(c));
        vo.setContent(c.getContent());
        vo.setIp(c.getIp());
        vo.setIpLocation(c.getIpLocation());
        vo.setStatus(c.getStatus());
        vo.setIsTop(topFlagOf(c));
        vo.setCreateTime(c.getCreateTime());
        return vo;
    }

    /** 历史数据的 author_type 可能为空，按最保守的「访客」处理 */
    private Integer authorTypeOf(BlogComment c) {
        return c.getAuthorType() == null ? BlogCommentConsts.AUTHOR_TYPE_VISITOR : c.getAuthorType();
    }

    /**
     * 置顶标记归一化：把库里的 NULL 统一成 0。
     *
     * <p>列是 {@code NOT NULL DEFAULT 0}，正常不会为 NULL；但改表前写入的历史行可能为空，
     * 直接下发 null 会让前端 {@code item.isTop} 为假值，与「未置顶」表现一致但仍多一种取值，
     * 不如在这里收敛。</p>
     */
    private Integer topFlagOf(BlogComment c) {
        return isTop(c) ? 1 : 0;
    }

    /**
     * 上溯到所在「顶级评论」的 id。
     *
     * <p>终止条件：{@code parentId} 为 0，或父评论不在 map 里（已删除 / 被过滤掉）。
     * 后者意味着该评论在当前树里已无父节点，应当提升为顶级。</p>
     *
     * <p>用 <b>迭代 + 记忆化</b> 实现：既能处理任意深的父链，也避免逐条评论反复回溯
     * （最坏情况退化成 O(n²)），同时不引入递归爆栈风险。</p>
     */
    private Long resolveRootId(BlogComment start, Map<Long, BlogComment> map, Map<Long, Long> cache) {
        Deque<Long> path = new ArrayDeque<>();
        BlogComment cur = start;
        Long rootId;
        while (true) {
            Long cached = cache.get(cur.getId());
            if (cached != null) {
                rootId = cached;
                break;
            }
            // 父评论必然先于子评论存在（id 自增），正常数据不可能成环；
            // 这里防的是外部导入的脏数据把请求线程永久挂住
            if (path.size() > map.size()) {
                rootId = start.getId();
                break;
            }
            Long parentId = cur.getParentId();
            BlogComment parent = (parentId == null || parentId == 0L) ? null : map.get(parentId);
            if (parent == null) {
                rootId = cur.getId();
                break;
            }
            path.push(cur.getId());
            cur = parent;
        }
        cache.put(cur.getId(), rootId);
        for (Long id : path) {
            cache.put(id, rootId);
        }
        return rootId;
    }

    /** id -> 昵称，用于把「回复了谁」由 parentId 解析成昵称，避免逐条评论重复扫描 */
    private Map<Long, String> nicknameIndex(List<BlogComment> list) {
        Map<Long, String> index = new HashMap<>(Math.max(16, list.size() * 2));
        for (BlogComment c : list) {
            index.put(c.getId(), c.getNickname());
        }
        return index;
    }

    /** 取被回复者的昵称；父评论不在当前集合里时返回 null（不展示「回复某某」） */
    private String replyToName(Long parentId, Map<Long, String> nicknames) {
        if (parentId == null || parentId == 0L) {
            return null;
        }
        return nicknames.get(parentId);
    }

    /**
     * 访客评论总开关校验：站点级关闭时直接拒绝写入。
     *
     * <p>与 {@code blog_post.allow_comment} 是「与」关系，两级都要开启才能评论：
     * 这里是站点级（后台「站点设置」），单篇级在 {@link #addPublic} 里按文章判断。
     * 提示语刻意区分，便于访客判断是站点关了还是这篇关了。</p>
     *
     * <p><b>只用于访客入口</b>（{@link #addPublic}）：后台的回复与主动评论由站长发起，
     * 不受这两个开关约束，理由见 {@link #addByOwner}。</p>
     */
    private void assertCommentEnabled() {
        if (!settingService.isCommentEnabled()) {
            throw new BizException("站点已关闭评论功能");
        }
    }

    @Override
    public void addPublic(PublicCommentSaveDTO dto) {
        if (dto.getPostId() == null) {
            throw new BizException("文章ID不能为空");
        }
        // 先清洗再校验：全角空格 / 零宽字符 / 首尾空白都不该让「博主」蒙混过关
        String nickname = NicknameUtil.sanitize(dto.getNickname());
        if (nickname.isEmpty()) {
            throw new BizException("昵称不能为空");
        }
        if (!notBlank(dto.getContent())) {
            throw new BizException("评论内容不能为空");
        }
        if (nickname.length() > BlogCommentConsts.MAX_NICKNAME_LENGTH) {
            throw new BizException("昵称过长");
        }
        if (dto.getContent().trim().length() > BlogCommentConsts.MAX_CONTENT_LENGTH) {
            throw new BizException("评论内容过长");
        }
        // 保留昵称拦截：**包含**即拒绝（后台可配 + 站长署名自动加入）。
        // 用包含而非相等，是为了挡住「博主小号」「我是站长」「admin01」这类夹带式冒充；
        // 真正的身份鉴别靠 author_type，这里只减少视觉混淆。
        String reservedHit = NicknameUtil.firstMatch(nickname, settingService.listReservedNicknames());
        if (reservedHit != null) {
            throw new BizException("昵称不能包含保留词「" + reservedHit + "」");
        }
        String email = normalizeEmail(dto.getEmail());
        // 全局总开关优先于验证码：站点关闭时立刻拒绝，避免白白消费掉一张验证码
        assertCommentEnabled();
        // 防刷准入：滑块验证码的二次校验，凭据一次性消费（见 CaptchaVerifier）。
        // 频率限制不在这里 —— 它在控制层的 @RateLimit 上，先于本方法执行
        // （见 BlogPublicController#submitComment）。
        captchaVerifier.assertVerified(dto.getCaptchaVerification());
        // 落库用的归一化 IP：与限流键同源（都用 ClientIpUtil），
        // 避免同一次请求在评论表和限流桶里被算成两个不同的 IP
        String ip = ClientIpUtil.current();
        BlogPost post = postMapper.selectById(dto.getPostId());
        if (post == null || !Objects.equals(post.getStatus(), SystemConsts.STATUS_ENABLE)) {
            throw new BizException("文章不存在或未发布");
        }
        if (!Objects.equals(post.getAllowComment(), BlogPostConsts.COMMENT_ENABLED)) {
            throw new BizException("该文章已关闭评论");
        }
        Long parentId = dto.getParentId() == null ? BlogCommentConsts.ROOT_PARENT_ID : dto.getParentId();
        if (parentId != BlogCommentConsts.ROOT_PARENT_ID) {
            BlogComment parent = commentMapper.selectById(parentId);
            if (parent == null || !Objects.equals(parent.getPostId(), dto.getPostId())) {
                throw new BizException("父评论不存在");
            }
            // 不限回复深度：显示层会压平成 2 层，parentId 只用于记录「回复了谁」
        }
        BlogComment comment = new BlogComment();
        comment.setPostId(dto.getPostId());
        comment.setParentId(parentId);
        comment.setNickname(nickname);
        // 访客身份一律强制为 0：不取前端传值，author_type 只由服务端决定
        comment.setAuthorType(BlogCommentConsts.AUTHOR_TYPE_VISITOR);
        comment.setEmail(email);
        comment.setContent(dto.getContent().trim());
        comment.setIp(ip);
        // 归属地在写入时解析并固化：即便日后更换数据文件，历史记录也保持当时的判定
        comment.setIpLocation(ipRegionResolver.lookup(ip));
        comment.setCommenterKey(CommenterKeyUtil.hash(dto.getCommenterKey()));
        comment.setStatus(BlogCommentConsts.STATUS_PENDING);
        commentMapper.insert(comment);
    }

    @Override
    public List<MyCommentVO> listMyComments(Long postId, String rawKey) {
        if (postId == null) {
            throw new BizException("文章ID不能为空");
        }
        String keyHash = CommenterKeyUtil.hash(CommenterKeyUtil.requireKey(rawKey));
        List<BlogComment> list = commentMapper.selectList(new LambdaQueryWrapper<BlogComment>()
                .eq(BlogComment::getPostId, postId)
                .eq(BlogComment::getCommenterKey, keyHash)
                .eq(BlogComment::getStatus, 0)
                .orderByDesc(BlogComment::getId));
        return list.stream().map(c -> {
            MyCommentVO vo = new MyCommentVO();
            vo.setId(c.getId());
            vo.setParentId(c.getParentId());
            vo.setContent(c.getContent());
            vo.setStatus(c.getStatus());
            vo.setCreateTime(c.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public void removeMine(Long id, String rawKey) {
        if (id == null) {
            throw new BizException("评论ID不能为空");
        }
        String keyHash = CommenterKeyUtil.hash(CommenterKeyUtil.requireKey(rawKey));
        BlogComment exist = commentMapper.selectById(id);
        if (exist == null || !notBlank(exist.getCommenterKey()) || !exist.getCommenterKey().equals(keyHash)) {
            throw new BizException("评论不存在或无权删除");
        }
        commentMapper.deleteById(id);
    }

    @Override
    public PageResult<PublicCommentVO> listApprovedPage(Long postId, PageQuery query) {
        List<PublicCommentVO> roots = listApprovedTree(postId);
        long total = roots.size();
        long pageNum = query.getPageNum();
        long pageSize = query.getPageSize();
        int from = (int) Math.min(total, (pageNum - 1) * pageSize);
        int to = (int) Math.min(total, (long) from + pageSize);
        List<PublicCommentVO> records = from >= total
                ? new ArrayList<>()
                : new ArrayList<>(roots.subList(from, to));
        return new PageResult<>(total, pageNum, pageSize, records);
    }

    /**
     * 校验收件邮箱（可选字段）。
     *
     * <p>前端 {@code maxlength} 对直连 API 无效，这里必须兜住：库列是 VARCHAR(100)，
     * 超长在 MySQL 严格模式会直接报错、非严格模式会被静默截断。</p>
     */
    private String normalizeEmail(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String email = raw.trim();
        if (email.length() > BlogCommentConsts.MAX_EMAIL_LENGTH) {
            throw new BizException("邮箱过长");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new BizException("邮箱格式不正确");
        }
        return email;
    }

    private void fillPostTitle(List<BlogComment> comments) {
        if (comments == null || comments.isEmpty()) {
            return;
        }
        List<Long> postIds = comments.stream()
                .map(BlogComment::getPostId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (postIds.isEmpty()) {
            return;
        }
        Map<Long, String> titleMap = postMapper.selectByIds(postIds).stream()
                .collect(Collectors.toMap(BlogPost::getId, BlogPost::getTitle, (a, b) -> a));
        for (BlogComment c : comments) {
            c.setPostTitle(titleMap.get(c.getPostId()));
        }
    }

    /** 审核状态是否合法（通过 / 拒绝） */
    private static boolean isValidAuditStatus(Integer status) {
        if (status == null) {
            return false;
        }
        return status == BlogCommentConsts.STATUS_APPROVED || status == BlogCommentConsts.STATUS_REJECTED;
    }

    /** 置顶标记是否合法（0 或 1） */
    private static boolean isValidTopFlag(Integer flag) {
        if (flag == null) {
            return false;
        }
        return flag == BlogCommentConsts.TOP_FLAG_OFF || flag == BlogCommentConsts.TOP_FLAG_ON;
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
