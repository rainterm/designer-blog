package com.designer.admin.framework.message;

import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Init;
import org.noear.solon.core.util.RunUtil;
import org.noear.solon.web.sse.SseEmitter;
import org.noear.solon.web.sse.SseEvent;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 站内消息 SSE 连接中心（单机内存；多实例可再接 Redis Pub/Sub）
 *
 * @author 雨夜终端
 */
@Component
@Slf4j
public class NoticeSseHub {

    /** userId -> emitters（同用户多标签页） */
    private final Map<Long, CopyOnWriteArraySet<SseEmitter>> sessions = new ConcurrentHashMap<>();

    /** SSE 心跳间隔（毫秒）：需小于前端重连阈值 */
    private static final long HEARTBEAT_INTERVAL_MILLIS = 25_000L;

    @Init
    public void init() {
        // 复用 Solon 的全局调度线程池：随 SolonApp 一起关闭，不需要自己管生命周期
        RunUtil.scheduleAtFixedRate(this::pingAll, HEARTBEAT_INTERVAL_MILLIS, HEARTBEAT_INTERVAL_MILLIS);
    }

    /** 建立订阅：不超时，由心跳 + 前端重连保活 */
    public SseEmitter open(Long userId) {
        final SseEmitter[] box = new SseEmitter[1];
        SseEmitter emitter = new SseEmitter(-1L)
                .onCompletion(() -> remove(userId, box[0]))
                .onError(e -> remove(userId, box[0]))
                .onInited(s -> {
                    box[0] = s;
                    sessions.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(s);
                    try {
                        s.send(new SseEvent().name("connected").data("ok").reconnectTime(3000L));
                    } catch (Exception e) {
                        log.debug("SSE 初始推送失败 userId={}", userId, e);
                    }
                });
        box[0] = emitter;
        return emitter;
    }

    public void pushToUsers(Iterable<Long> userIds, String eventName, String json) {
        if (userIds == null) {
            return;
        }
        for (Long userId : userIds) {
            pushToUser(userId, eventName, json);
        }
    }

    public void pushToAllOnline(String eventName, String json) {
        for (Long userId : sessions.keySet()) {
            pushToUser(userId, eventName, json);
        }
    }

    public void pushToUser(Long userId, String eventName, String json) {
        if (userId == null) {
            return;
        }
        Set<SseEmitter> set = sessions.get(userId);
        if (set == null || set.isEmpty()) {
            return;
        }
        SseEvent event = new SseEvent().name(eventName).data(json).reconnectTime(3000L);
        for (SseEmitter emitter : set) {
            try {
                emitter.send(event);
            } catch (Exception e) {
                remove(userId, emitter);
            }
        }
    }

    private void pingAll() {
        // 定时任务里抛出的异常会取消后续调度，这里整体兜一层
        try {
            SseEvent ping = new SseEvent().name("ping").data(String.valueOf(System.currentTimeMillis()));
            for (Map.Entry<Long, CopyOnWriteArraySet<SseEmitter>> e : sessions.entrySet()) {
                for (SseEmitter emitter : e.getValue()) {
                    try {
                        emitter.send(ping);
                    } catch (Exception ex) {
                        remove(e.getKey(), emitter);
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("SSE 心跳推送失败", ex);
        }
    }

    private void remove(Long userId, SseEmitter emitter) {
        if (userId == null || emitter == null) {
            return;
        }
        CopyOnWriteArraySet<SseEmitter> set = sessions.get(userId);
        if (set == null) {
            return;
        }
        set.remove(emitter);
        try {
            emitter.complete();
        } catch (Exception ignored) {
            // ignore
        }
        if (set.isEmpty()) {
            sessions.remove(userId, set);
        }
    }
}
