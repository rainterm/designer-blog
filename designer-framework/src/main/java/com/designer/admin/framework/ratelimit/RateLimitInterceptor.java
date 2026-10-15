package com.designer.admin.framework.ratelimit;

import cn.dev33.satoken.stp.StpUtil;
import com.designer.admin.framework.client.ClientIpUtil;
import com.designer.admin.framework.redis.RedisCounter;
import com.designer.admin.framework.satoken.LoginHelper;
import org.noear.solon.Solon;
import org.noear.solon.core.aspect.Invocation;
import org.noear.solon.core.aspect.MethodInterceptor;
import org.noear.solon.core.handle.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link RateLimit} 的执行者：把注解上的规则逐条落到 Redis 计数上。
 *
 * <p>由 Solon 的 {@code @Around} 机制挂载（见 {@link RateLimit}），
 * 实例在容器启动时创建一次，运行时无状态。</p>
 *
 * <p><b>可用性优先</b>：限流是「保护」不是「正确性」，所以两处刻意 fail-open
 * 并打日志 —— Redis 未装配时跳过限流、按请求头取值取不到时跳过该条规则。
 * 让 Redis 抖动或一个配错的注解把业务接口变成 500 是本末倒置。</p>
 */
public class RateLimitInterceptor implements MethodInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    /**
     * 限流键前缀。
     *
     * <p>与业务自己的计数键分开命名空间：限流键由框架统一回收（固定窗口到期即消失），
     * 而业务键的生命周期由业务决定，混在一起会让「谁该过期」变得难以判断。</p>
     */
    private static final String KEY_PREFIX = "rate:";

    private static final String DEFAULT_MESSAGE = "操作过于频繁，请稍后再试";

    @Override
    public Object doIntercept(Invocation inv) throws Throwable {
        RateLimit anno = resolveAnnotation(inv);
        if (anno == null) {
            return inv.invoke();
        }

        RedisCounter counter = Solon.context().getBean(RedisCounter.class);
        if (counter == null) {
            log.warn("[rate-limit] 未装配 RedisCounter，跳过限流：{}", anno.name());
            return inv.invoke();
        }

        Context ctx = Context.current();
        String subject = subject(anno, ctx);
        if (subject == null) {
            // 维度取不到值（例如按请求头限流但请求没带该头）：整条注解都跳过
            log.debug("[rate-limit] {} 维度 {} 取不到键，跳过限流", anno.name(), anno.dimension());
            return inv.invoke();
        }

        for (RateLimit.Rule rule : anno.rules()) {
            String key = buildKey(anno, rule, subject);
            long count = counter.incrWithTtl(key, rule.window());
            if (count > rule.count()) {
                return exceed(anno, rule, counter, key);
            }
        }

        return inv.invoke();
    }

    /** 取方法上的注解，退回类上的（与 Solon 自身的 {@code CloudBreakerInterceptor} 一致） */
    private RateLimit resolveAnnotation(Invocation inv) {
        RateLimit anno = inv.getMethodAnnotation(RateLimit.class);
        if (anno == null) {
            anno = inv.getTargetAnnotation(RateLimit.class);
        }
        return anno;
    }

    /**
     * 生成限流键：{@code rate:{name}:{维度主体}:{count}x{window}}。
     *
     * <p>规则部分用 {@code count x window} 而不是数组下标：下标会随「调整规则顺序」
     * 悄悄指向另一个桶，而 {@code 20x86400} 是自描述的 —— 看到键就知道是谁的额度。
     * 代价是改阈值等于换新桶（旧计数自然作废），这对限流来说反而更符合预期。</p>
     */
    private String buildKey(RateLimit anno, RateLimit.Rule rule, String subject) {
        return KEY_PREFIX + anno.name() + ":" + subject + ":" + rule.count() + "x" + rule.window();
    }

    /**
     * 解析「按谁计数」。
     *
     * @return 维度主体；无法取值时返回 {@code null}（整条注解跳过）
     */
    private String subject(RateLimit anno, Context ctx) {
        return switch (anno.dimension()) {
            case IP -> "ip:" + ClientIpUtil.normalize(ctx);
            case GLOBAL -> "all";
            case USER -> StpUtil.isLogin()
                    // 未登录退化为按 IP，避免所有匿名访客共享一个「匿名」桶
                    ? "user:" + LoginHelper.getUserId()
                    : "ip:" + ClientIpUtil.normalize(ctx);
            case HEADER -> headerSubject(anno, ctx);
        };
    }

    private String headerSubject(RateLimit anno, Context ctx) {
        String headerName = anno.headerName();
        if (headerName == null || headerName.isBlank()) {
            log.error("[rate-limit] {} 使用 HEADER 维度但未配置 headerName，跳过限流", anno.name());
            return null;
        }
        String value = ctx == null ? null : ctx.header(headerName);
        return (value == null || value.isBlank()) ? null : "hdr:" + value.trim();
    }

    /** 超限：按注解配置抛异常或静默跳过 */
    private Object exceed(RateLimit anno, RateLimit.Rule rule, RedisCounter counter, String key) {
        // 用 debug 而不是 warn：限流被触发时往往正是被刷的时候，
        // 按请求打 warn 会把日志淹掉，反而看不到真正的故障。
        log.debug("[rate-limit] 触发限流 name={} key={} 上限={}", anno.name(), key, rule.count());

        if (anno.exceed() == RateLimitExceed.SILENT) {
            return null;
        }
        // 只剩提示语时才去读一次 TTL（被拒是低频路径，多一次 Redis 往返可以接受）
        throw new RateLimitException(buildMessage(rule, counter.ttlSeconds(key)));
    }

    private String buildMessage(RateLimit.Rule rule, long remainSeconds) {
        String raw = rule.message();
        if (raw == null || raw.isBlank()) {
            return DEFAULT_MESSAGE;
        }
        if (raw.contains("{}")) {
            return raw.replace("{}", String.valueOf(Math.max(remainSeconds, 1)));
        }
        return raw;
    }
}
