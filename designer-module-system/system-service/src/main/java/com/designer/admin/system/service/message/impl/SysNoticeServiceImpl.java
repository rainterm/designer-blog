package com.designer.admin.system.service.message.impl;

import com.designer.admin.common.constant.SystemConsts;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.designer.admin.common.core.PageQuery;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.common.util.JsonUtil;
import com.designer.admin.framework.message.InboxSink;
import com.designer.admin.framework.notice.NoticeSseHub;
import com.designer.admin.framework.satoken.LoginHelper;
import com.designer.admin.system.constant.SysNoticeConsts;
import com.designer.admin.system.mapper.message.SysNoticeMapper;
import com.designer.admin.system.mapper.message.SysNoticeReadMapper;
import com.designer.admin.system.mapper.rbac.SysUserMapper;
import com.designer.admin.system.model.entity.message.SysNotice;
import com.designer.admin.system.model.entity.message.SysNoticeRead;
import com.designer.admin.system.model.entity.rbac.SysUser;
import com.designer.admin.system.model.vo.message.NoticePushVO;
import com.designer.admin.system.service.message.SysNoticeService;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.data.annotation.Transaction;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 站内消息：通道 inbox 投递落库 + 顶部消息中心 + SSE 推送；实现 framework {@link InboxSink}。
 *
 * @author 雨夜终端
 */
@Component
public class SysNoticeServiceImpl implements SysNoticeService, InboxSink {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Db
    private SysNoticeMapper noticeMapper;
    @Db
    private SysNoticeReadMapper noticeReadMapper;
    @Db
    private SysUserMapper userMapper;

    @Inject
    private NoticeSseHub noticeSseHub;

    @Override
    public Long publish(String title, String content, String receivers) {
        return publishInbox(title, content, receivers, null);
    }

    @Override
    public Long publish(String title, String content, String receivers, String link) {
        return publishInbox(title, content, receivers, link);
    }

    @Override
    @Transaction
    public Long publishInbox(String title, String content, String receivers, String link) {
        SysNotice notice = new SysNotice();
        notice.setTitle(title);
        notice.setContent(content == null ? "" : content);
        notice.setStatus(SystemConsts.STATUS_ENABLE);
        notice.setReceivers(receivers == null ? "" : receivers.trim());
        if (link != null && !link.isBlank()) {
            notice.setRemark(SysNoticeConsts.LINK_PREFIX + link.trim());
            notice.setLink(link.trim());
        } else {
            notice.setRemark("站内通道投递");
        }
        notice.setCreateTime(LocalDateTime.now());
        noticeMapper.insert(notice);
        pushNewNotice(notice);
        return notice.getId();
    }

    @Override
    @Transaction
    public Long publishInbox(String title, String content, String receivers) {
        return publishInbox(title, content, receivers, null);
    }

    @Override
    public IPage<SysNotice> listInboxPage(PageQuery query) {
        String username = LoginHelper.getUsername();
        Long userId = LoginHelper.getUserId();
        List<SysNotice> all = visiblePublished(username, userId);
        long total = all.size();
        long from = Math.max(0, (query.getPageNum() - 1) * query.getPageSize());
        long to = Math.min(total, from + query.getPageSize());
        List<SysNotice> slice = from >= total ? List.of() : all.subList((int) from, (int) to);
        Set<Long> readIds = readNoticeIds(userId, slice.stream().map(SysNotice::getId).toList());
        for (SysNotice n : slice) {
            n.setReadFlag(readIds.contains(n.getId()));
            n.setLink(parseLink(n.getRemark()));
        }
        Page<SysNotice> page = new Page<>(query.getPageNum(), query.getPageSize(), total);
        page.setRecords(slice);
        return page;
    }

    @Override
    public long countUnread() {
        String username = LoginHelper.getUsername();
        Long userId = LoginHelper.getUserId();
        List<SysNotice> list = visiblePublished(username, userId);
        if (list.isEmpty()) {
            return 0;
        }
        Set<Long> readIds = readNoticeIds(userId, list.stream().map(SysNotice::getId).toList());
        return list.stream().filter(n -> !readIds.contains(n.getId())).count();
    }

    @Override
    @Transaction
    public void markRead(Long noticeId) {
        if (noticeId == null) {
            throw new BizException("消息ID不能为空");
        }
        SysNotice notice = noticeMapper.selectById(noticeId);
        if (notice == null || notice.getStatus() == null || notice.getStatus() != 1) {
            throw new BizException("消息不存在或已关闭");
        }
        if (!visibleTo(notice, LoginHelper.getUsername(), LoginHelper.getUserId())) {
            throw new BizException("无权查看该消息");
        }
        Long userId = LoginHelper.getUserId();
        Long exists = noticeReadMapper.selectCount(new LambdaQueryWrapper<SysNoticeRead>()
                .eq(SysNoticeRead::getNoticeId, noticeId)
                .eq(SysNoticeRead::getUserId, userId));
        if (exists != null && exists > 0) {
            return;
        }
        SysNoticeRead read = new SysNoticeRead();
        read.setNoticeId(noticeId);
        read.setUserId(userId);
        read.setReadTime(LocalDateTime.now());
        noticeReadMapper.insert(read);
    }

    @Override
    @Transaction
    public int markAllRead() {
        String username = LoginHelper.getUsername();
        Long userId = LoginHelper.getUserId();
        List<SysNotice> list = visiblePublished(username, userId);
        if (list.isEmpty()) {
            return 0;
        }
        Set<Long> readIds = readNoticeIds(userId, list.stream().map(SysNotice::getId).toList());
        int count = 0;
        for (SysNotice n : list) {
            if (readIds.contains(n.getId())) {
                continue;
            }
            SysNoticeRead read = new SysNoticeRead();
            read.setNoticeId(n.getId());
            read.setUserId(userId);
            read.setReadTime(LocalDateTime.now());
            noticeReadMapper.insert(read);
            count++;
        }
        return count;
    }

    private void pushNewNotice(SysNotice notice) {
        String link = notice.getLink() != null ? notice.getLink() : parseLink(notice.getRemark());
        NoticePushVO payload = NoticePushVO.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .content(truncate(notice.getContent(), 200))
                .createTime(notice.getCreateTime() == null ? "" : TIME_FMT.format(notice.getCreateTime()))
                .link(link)
                .build();
        String json = JsonUtil.toJson(payload);

        String receivers = notice.getReceivers();
        if (!notBlank(receivers) || SysNoticeConsts.RECEIVERS_ALL.equals(receivers.trim())) {
            noticeSseHub.pushToAllOnline("notice", json);
            return;
        }
        List<String> tokens = Arrays.stream(receivers.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
        if (tokens.isEmpty()) {
            noticeSseHub.pushToAllOnline("notice", json);
            return;
        }
        List<Long> userIds = resolveReceiverUserIds(tokens);
        if (userIds.isEmpty()) {
            return;
        }
        noticeSseHub.pushToUsers(userIds, "notice", json);
    }

    /**
     * receivers 支持用户 ID 或登录账号（逗号分隔）；审批模块传 ID，消息通道可能传账号。
     */
    private List<Long> resolveReceiverUserIds(List<String> tokens) {
        Set<Long> ids = new java.util.LinkedHashSet<>();
        List<String> names = new java.util.ArrayList<>();
        for (String token : tokens) {
            try {
                ids.add(Long.valueOf(token));
            } catch (NumberFormatException e) {
                names.add(token);
            }
        }
        if (!names.isEmpty()) {
            userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                            .in(SysUser::getUsername, names)
                            .eq(SysUser::getStatus, 1)
                            .select(SysUser::getId))
                    .stream()
                    .map(SysUser::getId)
                    .filter(id -> id != null)
                    .forEach(ids::add);
        }
        if (ids.isEmpty()) {
            return List.of();
        }
        return userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .in(SysUser::getId, ids)
                        .eq(SysUser::getStatus, 1)
                        .select(SysUser::getId))
                .stream()
                .map(SysUser::getId)
                .toList();
    }

    private static String parseLink(String remark) {
        if (remark == null || remark.isBlank() || !remark.startsWith(SysNoticeConsts.LINK_PREFIX)) {
            return null;
        }
        String link = remark.substring(SysNoticeConsts.LINK_PREFIX.length()).trim();
        return link.isEmpty() ? null : link;
    }

    private String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() > max ? text.substring(0, max) : text;
    }

    private List<SysNotice> visiblePublished(String username, Long userId) {
        return noticeMapper.selectList(new LambdaQueryWrapper<SysNotice>()
                        .eq(SysNotice::getStatus, 1)
                        .orderByDesc(SysNotice::getId))
                .stream()
                .filter(n -> visibleTo(n, username, userId))
                .toList();
    }

    private boolean visibleTo(SysNotice notice, String username, Long userId) {
        if (notice == null) {
            return false;
        }
        String receivers = notice.getReceivers();
        if (!notBlank(receivers) || SysNoticeConsts.RECEIVERS_ALL.equals(receivers.trim())) {
            return true;
        }
        String uid = userId == null ? null : String.valueOf(userId);
        return Arrays.stream(receivers.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .anyMatch(token -> (username != null && username.equals(token))
                        || (uid != null && uid.equals(token)));
    }

    private Set<Long> readNoticeIds(Long userId, List<Long> noticeIds) {
        if (noticeIds == null || noticeIds.isEmpty()) {
            return Set.of();
        }
        return noticeReadMapper.selectList(new LambdaQueryWrapper<SysNoticeRead>()
                        .eq(SysNoticeRead::getUserId, userId)
                        .in(SysNoticeRead::getNoticeId, noticeIds))
                .stream()
                .map(SysNoticeRead::getNoticeId)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
