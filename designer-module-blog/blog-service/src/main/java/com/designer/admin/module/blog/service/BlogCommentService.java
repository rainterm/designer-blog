package com.designer.admin.module.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.common.core.PageQuery;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.framework.captcha.CaptchaVerifier;
import com.designer.admin.framework.client.ClientIpUtil;
import com.designer.admin.framework.client.IpRegionResolver;
import com.designer.admin.module.blog.mapper.BlogCommentMapper;
import com.designer.admin.module.blog.mapper.BlogPostMapper;
import com.designer.admin.module.blog.model.dto.CommentAuditDTO;
import com.designer.admin.module.blog.model.dto.CommentQueryDTO;
import com.designer.admin.module.blog.model.dto.CommentReplyDTO;
import com.designer.admin.module.blog.model.dto.PublicCommentSaveDTO;
import com.designer.admin.module.blog.model.entity.BlogComment;
import com.designer.admin.module.blog.model.entity.BlogPost;
import com.designer.admin.module.blog.model.vo.MyCommentVO;
import com.designer.admin.module.blog.model.vo.PublicCommentVO;
import com.designer.admin.module.blog.util.CommenterKeyUtil;
import com.designer.admin.module.blog.util.NicknameUtil;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class BlogCommentService {

    /** 宽松的邮箱格式校验：只要「本地部分@域名.后缀」即可，不追求 RFC 完整 */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    /** 内容长度上限，与 blog_comment.content 列宽一致（VARCHAR(2000)） */
    private static final int MAX_CONTENT_LENGTH = 2000;
    /** 昵称长度上限，与 blog_comment.nickname 列宽一致（VARCHAR(50)） */
    private static final int MAX_NICKNAME_LENGTH = 50;
    /** 作者类型：访客 */
    private static final int AUTHOR_TYPE_VISITOR = 0;
    /** 作者类型：博主 */
    private static final int AUTHOR_TYPE_OWNER = 1;

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

    public IPage<BlogComment> page(CommentQueryDTO query) {
        IPage<BlogComment> page = commentMapper.selectPage(query.toPage(), new LambdaQueryWrapper<BlogComment>()
                .eq(query.getPostId() != null, BlogComment::getPostId, query.getPostId())
                .like(notBlank(query.getNickname()), BlogComment::getNickname, query.getNickname())
                .eq(query.getStatus() != null, BlogComment::getStatus, query.getStatus())
                .and(notBlank(query.getKeyword()), w -> w
                        .like(BlogComment::getContent, query.getKeyword())
                        .or()
                        .like(BlogComment::getNickname, query.getKeyword()))
                .orderByDesc(BlogComment::getId));
        fillPostTitle(page.getRecords());
        return page;
    }

    public void audit(CommentAuditDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("评论ID不能为空");
        }
        if (dto.getStatus() == null || (dto.getStatus() != 1 && dto.getStatus() != 2)) {
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

    public void remove(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        commentMapper.deleteByIds(ids);
    }

    /**
     * 管理员回复评论：挂到目标评论下，直接以「已通过」落库，无需再审。
     *
     * <p>与访客提交的区别：</p>
     * <ul>
     *   <li>不走验证码与频率限制——这是站长的正常运营动作；</li>
     *   <li>{@code status} 直接置 1，回复即刻对访客可见；</li>
     *   <li><b>不记录 IP / 归属地</b>：回复代表站点方，公开归属地会暴露站长的位置信息，没有展示价值。</li>
     * </ul>
     *
     * <p>层级不再限制：显示层统一压平为 2 层，回复的回复也挂在同一条顶级评论下，
     * 由 {@code replyToNickname} 标出「回复了谁」。</p>
     */
    public void reply(CommentReplyDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("评论ID不能为空");
        }
        if (!notBlank(dto.getContent())) {
            throw new BizException("回复内容不能为空");
        }
        String content = dto.getContent().trim();
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new BizException("回复内容过长");
        }
        // 全局关闭时前台整个输入区都不渲染，此时允许回复只会造出「看不见的评论」
        assertCommentEnabled();
        BlogComment parent = commentMapper.selectById(dto.getId());
        if (parent == null) {
            throw new BizException("评论不存在");
        }
        // 只允许回复已通过的评论：回复会以 status=1 立即公开，若父评论仍在待审/已拒绝，
        // 公开列表会因父评论被过滤而把这条回复「孤儿提升」成顶级评论，
        // 读者会看到一句没有上下文的博主发言。先审核再回复，行为才可预期。
        if (!Objects.equals(parent.getStatus(), 1)) {
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
        reply.setAuthorType(AUTHOR_TYPE_OWNER);
        reply.setEmail("");
        reply.setContent(content);
        reply.setIp("");
        reply.setIpLocation("");
        // 管理员回复不携带访客密钥，避免被识别为某个浏览器的「我的评论」
        reply.setCommenterKey("");
        reply.setStatus(1);
        commentMapper.insert(reply);
    }

    /**
     * 公开评论树：**只分两层**——顶级评论 + 其下压平的全部回复。
     *
     * <p>不论真实回复多深（回复的回复的回复…），一律挂到所在顶级评论下作为第 2 层，
     * 并通过 {@code replyToNickname} 标出「回复了谁」。这样：</p>
     * <ul>
     *   <li>前台缩进最多一级，不会出现越缩越窄的深链；</li>
     *   <li>同一串对话平铺在一起，不必逐层展开；</li>
     *   <li>父评论未通过时，子评论提升为顶级（保持原有语义）。</li>
     * </ul>
     */
    public List<PublicCommentVO> listApprovedTree(Long postId) {
        List<BlogComment> list = commentMapper.selectList(new LambdaQueryWrapper<BlogComment>()
                .eq(BlogComment::getPostId, postId)
                .eq(BlogComment::getStatus, 1)
                .orderByAsc(BlogComment::getId));
        // LinkedHashMap：保持 id 升序，让顶级评论与回复都按时间先后排列
        Map<Long, PublicCommentVO> map = new LinkedHashMap<>();
        for (BlogComment c : list) {
            PublicCommentVO vo = new PublicCommentVO();
            vo.setId(c.getId());
            vo.setParentId(c.getParentId());
            vo.setNickname(c.getNickname());
            vo.setAuthorType(c.getAuthorType() == null ? AUTHOR_TYPE_VISITOR : c.getAuthorType());
            vo.setContent(c.getContent());
            vo.setIpLocation(c.getIpLocation());
            vo.setCreateTime(c.getCreateTime());
            map.put(vo.getId(), vo);
        }

        List<PublicCommentVO> roots = new ArrayList<>();
        Map<Long, Long> rootIdCache = new HashMap<>();
        for (PublicCommentVO vo : map.values()) {
            Long rootId = resolveRootId(vo, map, rootIdCache);
            if (rootId.equals(vo.getId())) {
                roots.add(vo);
                continue;
            }
            PublicCommentVO root = map.get(rootId);
            if (root == null) {
                // 理论不可达；兜底为顶级，避免评论凭空消失
                roots.add(vo);
                continue;
            }
            vo.setReplyToNickname(nicknameOf(vo.getParentId(), map));
            root.getChildren().add(vo);
        }
        return roots;
    }

    /**
     * 上溯到所在「顶级评论」的 id。
     *
     * <p>终止条件：{@code parentId} 为 0，或父评论不在 map 里（未通过 / 已删除）。
     * 后者意味着该评论在可见树里已无父节点，应当提升为顶级。</p>
     *
     * <p>用 <b>迭代 + 记忆化</b> 实现：既能处理任意深的父链，也避免逐条评论反复回溯
     * （最坏情况退化成 O(n²)），同时不引入递归爆栈风险。</p>
     */
    private Long resolveRootId(PublicCommentVO start, Map<Long, PublicCommentVO> map, Map<Long, Long> cache) {
        Deque<Long> path = new ArrayDeque<>();
        PublicCommentVO cur = start;
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
            PublicCommentVO parent = (parentId == null || parentId == 0L) ? null : map.get(parentId);
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

    /** 取被回复者的昵称；父评论不在可见集合里时返回 null（不展示「回复某某」） */
    private String nicknameOf(Long parentId, Map<Long, PublicCommentVO> map) {
        if (parentId == null || parentId == 0L) {
            return null;
        }
        PublicCommentVO parent = map.get(parentId);
        return parent == null ? null : parent.getNickname();
    }

    /**
     * 评论总开关校验：站点级关闭时直接拒绝写入。
     *
     * <p>与 {@code blog_post.allow_comment} 是「与」关系，两级都要开启才能评论：
     * 这里是站点级（后台「站点设置」），单篇级在 {@link #submitPublic} 里按文章判断。
     * 提示语刻意区分，便于访客判断是站点关了还是这篇关了。</p>
     */
    private void assertCommentEnabled() {
        if (!settingService.isCommentEnabled()) {
            throw new BizException("站点已关闭评论功能");
        }
    }

    public void submitPublic(PublicCommentSaveDTO dto) {
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
        if (nickname.length() > MAX_NICKNAME_LENGTH) {
            throw new BizException("昵称过长");
        }
        if (dto.getContent().trim().length() > MAX_CONTENT_LENGTH) {
            throw new BizException("评论内容过长");
        }
        // 保留昵称拦截：**包含**即拒绝（后台可配 + 站长署名自动加入）。
        // 用包含而非相等，是为了挡住「博主小号」「我是站长」「admin01」这类夹带式冒充；
        // 真正的身份鉴别靠 author_type，这里只减少视觉混淆。
        String reservedHit = NicknameUtil.firstMatch(nickname, settingService.getReservedNicknames());
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
        if (post == null || !Objects.equals(post.getStatus(), 1)) {
            throw new BizException("文章不存在或未发布");
        }
        if (!Objects.equals(post.getAllowComment(), 1)) {
            throw new BizException("该文章已关闭评论");
        }
        Long parentId = dto.getParentId() == null ? 0L : dto.getParentId();
        if (parentId != 0L) {
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
        comment.setAuthorType(AUTHOR_TYPE_VISITOR);
        comment.setEmail(email);
        comment.setContent(dto.getContent().trim());
        comment.setIp(ip);
        // 归属地在写入时解析并固化：即便日后更换数据文件，历史记录也保持当时的判定
        comment.setIpLocation(ipRegionResolver.lookup(ip));
        comment.setCommenterKey(CommenterKeyUtil.hash(dto.getCommenterKey()));
        comment.setStatus(0);
        commentMapper.insert(comment);
    }

    /**
     * 评论人查看自己在某篇文章下的待审评论。
     *
     * <p>只返回 `status=0`：已通过的评论本来就在公开列表里，被拒的也不必再展示给作者。
     * 因为用匿名密钥识别身份，**必须**校验密钥；密钥为空的老数据永远不会被匹配到。</p>
     */
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

    /**
     * 评论人删除自己的评论：按 ID + 密钥哈希双重匹配，匹配不到一律报「不存在」。
     *
     * <p>关键安全点：密钥为空时必须直接拒绝。否则历史数据（`commenter_key = ''`）
     * 会被空密钥匹配上，任何人都能删掉它们。</p>
     */
    public void deleteMine(Long id, String rawKey) {
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

    /**
     * 分页版评论树：以**顶级评论**为单位分页，每条顶级评论带上它已通过的全部回复。
     *
     * <p>树形结构没法直接交给 SQL 分页，所以先在内存里按顶级整理，再切页。
     * 这样返回体大小随页大小收敛（一次性返回全部评论时，单篇被刷爆后响应体会非常大），
     * 同时保留原有语义：父评论未通过时子评论提升为顶级，避免漏展示。</p>
     */
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
        if (email.length() > 100) {
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

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
