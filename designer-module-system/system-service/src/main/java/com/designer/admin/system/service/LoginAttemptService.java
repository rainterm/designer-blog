package com.designer.admin.system.service;

import com.designer.admin.common.exception.BizException;
import com.designer.admin.framework.redis.RedisCounter;
import org.noear.redisx.RedisClient;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

/**
 * 登录失败计数与账号锁定，以及「本次登录是否需要人机验证」的判据。
 *
 * <p><b>为什么不叫 CaptchaService</b>：这个类原先叫 CaptchaService，把三件事混在一起 ——
 * 画算术题验证码、累计失败次数、锁定账号。验证码形态换成滑块后
 * （AJ-Captcha，见 {@code com.designer.admin.framework.captcha}），画图那部分整体删除：
 * 底图与拼图由插件现生成，凭据校验交给 {@code CaptchaVerifier}。
 * 唯一与验证码形态无关、因而必须留下的是<b>策略</b>——
 * 「这个账号失败够次数了吗，要不要拦一道人机验证」，以及配套的计数与锁定。
 * 于是改名为此，职责收窄为「登录尝试」本身。</p>
 *
 * <p><b>与 {@code CaptchaVerifier} 的分工</b>：这里只回答「要不要验」，
 * 「凭据是否有效」由 {@code CaptchaVerifier} 回答。调用方（{@code AuthService}）
 * 先问这里，需要验再去调那边，顺序不能反 —— 凭据是一次性的，
 * 对不需要验证的账号调一次校验只会把它白白消费掉。</p>
 */
@Component
public class LoginAttemptService {

    private static final String FAIL_KEY = "login:fail:";
    private static final String LOCK_KEY = "login:lock:";

    /** 连续失败达到该次数后才要求验证码（默认 3） */
    public static final int DEFAULT_CAPTCHA_FAIL_THRESHOLD = 3;

    @Inject
    private RedisClient redisClient;
    @Inject
    private RedisCounter redisCounter;
    @Inject
    private SysConfigService configService;

    /**
     * 是否需要校验人机验证：全局开启且该账号失败次数 ≥ 阈值。
     *
     * <p>滑块本身不需要「按账号生成」，所以这里只回答是 / 否，
     * 前端据此决定要不要调 {@code /captcha/get} 取一张。</p>
     */
    public boolean needCaptcha(String username) {
        if (!configService.getBool(SysConfigService.KEY_CAPTCHA_ENABLED, true)) {
            return false;
        }
        return getFailCount(username) >= captchaFailThreshold();
    }

    private int captchaFailThreshold() {
        int threshold = configService.getInt(
                SysConfigService.KEY_CAPTCHA_FAIL_THRESHOLD, DEFAULT_CAPTCHA_FAIL_THRESHOLD);
        return Math.max(1, threshold);
    }

    public long getFailCount(String username) {
        if (username == null || username.isBlank()) {
            return 0;
        }
        String failKey = FAIL_KEY + username.trim();
        String val = redisClient.open1(s -> s.key(failKey).get());
        if (val == null || val.isBlank()) {
            return 0;
        }
        try {
            return Long.parseLong(val.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public void assertNotLocked(String username) {
        if (!configService.getBool(SysConfigService.KEY_LOCK_ENABLED, true)) {
            return;
        }
        if (username == null || username.isBlank()) {
            return;
        }
        Boolean locked = redisClient.open1(s -> s.key(LOCK_KEY + username.trim()).exists());
        if (Boolean.TRUE.equals(locked)) {
            long ttl = redisClient.open1(s -> s.key(LOCK_KEY + username.trim()).ttl());
            long minutes = Math.max(1, (ttl + 59) / 60);
            throw new BizException("账号已锁定，请 " + minutes + " 分钟后再试");
        }
    }

    /**
     * 记录一次登录失败。始终累计失败次数（供验证码阈值）；锁定开启时达到上限则锁定。
     *
     * @return 当前失败次数
     */
    public long recordLoginFail(String username) {
        if (username == null || username.isBlank()) {
            return 0;
        }
        String user = username.trim();
        boolean lockEnabled = configService.getBool(SysConfigService.KEY_LOCK_ENABLED, true);
        boolean captchaEnabled = configService.getBool(SysConfigService.KEY_CAPTCHA_ENABLED, true);
        if (!lockEnabled && !captchaEnabled) {
            return 0;
        }
        int maxRetry = configService.getInt(SysConfigService.KEY_LOCK_MAX_RETRY, 5);
        int lockMinutes = configService.getInt(SysConfigService.KEY_LOCK_MINUTES, 10);
        // 失败计数 TTL：至少覆盖锁定时长，且不少于 30 分钟，避免验证码阈值过早丢失
        int failTtl = Math.max(lockMinutes * 60, 1800);
        String failKey = FAIL_KEY + user;
        long count = redisCounter.incrWithTtl(failKey, failTtl);
        if (lockEnabled && count >= maxRetry) {
            redisClient.open0(s -> {
                s.key(LOCK_KEY + user).expire(lockMinutes * 60).set("1");
                s.key(failKey).delete();
            });
            throw new BizException("登录失败次数过多，账号已锁定 " + lockMinutes + " 分钟");
        }
        return count;
    }

    public void clearLoginFail(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        String user = username.trim();
        redisClient.open0(s -> {
            s.key(FAIL_KEY + user).delete();
            s.key(LOCK_KEY + user).delete();
        });
    }
}
