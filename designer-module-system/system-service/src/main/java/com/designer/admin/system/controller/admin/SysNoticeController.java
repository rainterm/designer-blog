package com.designer.admin.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.designer.admin.common.core.PageQuery;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.R;
import com.designer.admin.framework.notice.NoticeSseHub;
import com.designer.admin.framework.satoken.LoginHelper;
import com.designer.admin.system.model.entity.SysNotice;
import com.designer.admin.system.service.SysNoticeService;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Post;
import org.noear.solon.validation.annotation.Valid;
import org.noear.solon.web.sse.SseEmitter;

import java.util.Map;

/**
 * 站内消息接收（顶部消息中心 + SSE）
 */
@Valid
@Controller
@Mapping("/admin-api/system/notice")
public class SysNoticeController {

    @Inject
    private SysNoticeService noticeService;

    @Inject
    private NoticeSseHub noticeSseHub;

    @Get
    @Mapping("/inbox")
    public R<PageResult<SysNotice>> inbox(PageQuery query) {
        return R.ok(PageResult.of(noticeService.inboxPage(query)));
    }

    @Get
    @Mapping("/unread-count")
    public R<Long> unreadCount() {
        return R.ok(noticeService.unreadCount());
    }

    @Post
    @Mapping("/read-all")
    public R<Map<String, Object>> markAllRead() {
        return R.ok(Map.of("count", noticeService.markAllRead()));
    }

    @Post
    @Mapping("/{id}/read")
    public R<Void> markRead(Long id) {
        noticeService.markRead(id);
        return R.ok();
    }

    /**
     * SSE 订阅。前端 EventSource 通过 query 传 Authorization（已开启 is-read-param）。
     */
    @Get
    @Mapping("/sse")
    @SaCheckLogin
    public SseEmitter sse() {
        return noticeSseHub.open(LoginHelper.getUserId());
    }
}
