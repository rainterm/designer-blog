package com.designer.admin.module.blog.service;

import com.designer.admin.module.blog.constant.BlogRedisKeyConsts;
import lombok.extern.slf4j.Slf4j;
import org.noear.redisx.RedisClient;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 阅读数计数器：Redis 累加，由 {@link BlogViewFlushJob} 定期回写 MySQL。
 *
 * <p>存在的意义是把「写」从读路径上摘掉：若文章详情接口在「读」的时候直接
 * {@code UPDATE blog_post SET view_count = view_count + 1}，一次廉价的 GET
 * 就变成一次数据库写 —— 热文章上会有行锁竞争，而且读写混在一起后，
 * 页面 HTML 也就彻底不能缓存了（缓存会让阅读数按命中次数失真）。</p>
 *
 * <p>读路径只碰 Redis（{@code HINCRBY}，O(1)、无行锁、不产生 redo），
 * 攒够一段时间再由定时任务合并成一条 {@code UPDATE ... + N}。N 次阅读 → 1 次写。</p>
 *
 * <p><b>为什么用 Hash 而不是「一堆独立 key + KEYS 扫描」</b>：Hash 天然就是
 * 「待回写文章 id 的集合」，可以直接 {@code HGETALL} 取一批，
 * 不需要 {@code KEYS blog:post:view:*}（那是 O(N) 且会阻塞 Redis）；
 * 也不必额外维护一个 dirty set。Redis 有持久化，进程重启后未回写的增量还在。</p>
 *
 * <p><b>为什么扣减用 {@code HINCRBY -delta} 而不是清零</b>：回写期间随时可能有新阅读进来。
 * 扣减只会减掉「本次已经落库的那部分」，期间新到的增量原样留到下一轮；
 * 若用 DEL/置零，这部分增量会被一起抹掉，表现为阅读数凭空少一截。</p>
 *
 * @author 雨夜终端
 */
@Component
@Slf4j
public class BlogViewCounter {

    @Inject
    private RedisClient redisClient;

    /** 记一次阅读。只碰 Redis，失败不影响访客（调用方已做异常收敛） */
    public void record(Long postId) {
        if (postId == null) {
            return;
        }
        redisClient.open0(s -> s.jedis().hincrBy(BlogRedisKeyConsts.POST_VIEW_PENDING, String.valueOf(postId), 1));
    }

    /**
     * 某篇文章尚未回写的增量。
     *
     * <p>详情接口的展示值 = 库里已落地的 + 它，否则访客会看到「自己这次阅读没算进去」。</p>
     */
    public long pending(Long postId) {
        if (postId == null) {
            return 0L;
        }
        return pendingBatch(List.of(postId)).getOrDefault(postId, 0L);
    }

    /**
     * 批量读未回写增量：一次 {@code HMGET}，列表页 5 篇也只多一次 Redis 往返。
     *
     * <p>列表同样要加上这部分，否则列表页与详情页显示的阅读数会差一截，
     * 看起来像「点进去阅读数就变了」。</p>
     *
     * @return postId -> 增量；只包含增量大于 0 的文章
     */
    public Map<Long, Long> pendingBatch(List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> ids = postIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        String[] fields = ids.stream().map(String::valueOf).toArray(String[]::new);
        List<String> values;
        try {
            values = redisClient.open1(s -> s.jedis().hmget(BlogRedisKeyConsts.POST_VIEW_PENDING, fields));
        } catch (Exception e) {
            // 降级为「没有未回写增量」：阅读数会显示成上一次回写的旧值，
            // 但不能因为 Redis 抖动就让文章列表 / 详情打不开。
            // 这是读路径，可用性优先于这几十次阅读的实时性。
            log.warn("[blog-view] 读取未回写增量失败，本次按 0 处理：{}", e.getMessage());
            return Collections.emptyMap();
        }
        Map<Long, Long> pending = HashMap.newHashMap(ids.size());
        for (int i = 0; i < ids.size(); i++) {
            String value = (values == null || i >= values.size()) ? null : values.get(i);
            long delta = parse(value);
            if (delta > 0) {
                pending.put(ids.get(i), delta);
            }
        }
        return pending;
    }

    /**
     * 取走当前全部待回写增量（只读，不修改 Redis —— 扣减由 {@link #settle} 在落库成功后做）。
     *
     * @return postId -> 增量；无待回写项时为空 Map
     */
    public Map<Long, Long> takeAll() {
        Map<String, String> raw = redisClient.open1(s -> s.jedis().hgetAll(BlogRedisKeyConsts.POST_VIEW_PENDING));
        Map<Long, Long> pending = HashMap.newHashMap(raw.size());
        for (Map.Entry<String, String> entry : raw.entrySet()) {
            long delta = parse(entry.getValue());
            if (delta <= 0) {
                continue;
            }
            try {
                pending.put(Long.parseLong(entry.getKey().trim()), delta);
            } catch (NumberFormatException ignored) {
                // field 不是合法文章 ID：跳过，不因为一条脏数据卡住整轮回写
            }
        }
        return pending;
    }

    /** 落库成功后扣减对应增量；失败时**不要**调用，让增量留在 Redis 下一轮重试 */
    public void settle(Long postId, long delta) {
        if (postId == null || delta <= 0) {
            return;
        }
        redisClient.open0(s -> s.jedis().hincrBy(BlogRedisKeyConsts.POST_VIEW_PENDING, String.valueOf(postId), -delta));
    }

    private long parse(String value) {
        if (value == null || value.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
