package com.designer.admin.system.service.auth.impl;

import com.designer.admin.common.exception.BizException;
import com.designer.admin.framework.redis.RedisCounter;
import com.designer.admin.system.constant.LoginAttemptConsts;
import com.designer.admin.system.constant.SysConfigConsts;
import com.designer.admin.system.constant.SystemRedisKeyConsts;
import com.designer.admin.system.service.auth.AuthService;
import com.designer.admin.system.service.auth.LoginAttemptService;
import com.designer.admin.system.service.config.SysConfigService;
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
 *
 * @author 雨夜终端
 */
@Component
public class LoginAttemptServiceImpl implements LoginAttemptService {

    @Inject
    private RedisClient redisClient;
    @Inject
    private RedisCounter redisCounter;
    @Inject
    private SysConfigService configService;

    @Override
    public boolean isCaptchaRequired(String username) {
        if (!configService.getBool(SysConfigConsts.KEY_CAPTCHA_ENABLED, true)) {
            return false;
        }
        return countLoginFail(username) >= captchaFailThreshold();
    }

    private int captchaFailThreshold() {
        int threshold = configService.getInt(
                SysConfigConsts.KEY_CAPTCHA_FAIL_THRESHOLD, SysConfigConsts.DEFAULT_CAPTCHA_FAIL_THRESHOLD);
        return Math.max(1, threshold);
    }

    @Override
    public long countLoginFail(String username) {
        if (username == null || username.isBlank()) {
            return 0;
        }
        String failKey = SystemRedisKeyConsts.LOGIN_FAIL_PREFIX + username.trim();
        String val = redisClient.openAndGet(s -> s.key(failKey).get());
        if (val == null || val.isBlank()) {
            return 0;
        }
        try {
            return Long.parseLong(val.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public void assertNotLocked(String username) {
        if (!configService.getBool(SysConfigConsts.KEY_LOCK_ENABLED, true)) {
            return;
        }
        if (username == null || username.isBlank()) {
            return;
        }
        String lockKey = SystemRedisKeyConsts.LOGIN_LOCK_PREFIX + username.trim();
        Boolean locked = redisClient.openAndGet(s -> s.key(lockKey).exists());
        if (Boolean.TRUE.equals(locked)) {
            long ttl = redisClient.openAndGet(s -> s.key(lockKey).ttl());
            long minutes = Math.max(1, (ttl + 59) / 60);
            throw new BizException("账号已锁定，请 " + minutes + " 分钟后再试");
        }
    }

    @Override
    public long saveLoginFail(String username) {
        if (username == null || username.isBlank()) {
            return 0;
        }
        String user = username.trim();
        boolean lockEnabled = configService.getBool(SysConfigConsts.KEY_LOCK_ENABLED, true);
        boolean captchaEnabled = configService.getBool(SysConfigConsts.KEY_CAPTCHA_ENABLED, true);
        if (!lockEnabled && !captchaEnabled) {
            return 0;
        }
        int maxRetry = configService.getInt(SysConfigConsts.KEY_LOCK_MAX_RETRY, SysConfigConsts.DEFAULT_LOCK_MAX_RETRY);
        int lockMinutes = configService.getInt(SysConfigConsts.KEY_LOCK_MINUTES, SysConfigConsts.DEFAULT_LOCK_MINUTES);
        // 失败计数 TTL：至少覆盖锁定时长，且不少于 30 分钟，避免验证码阈值过早丢失
        int failTtl = Math.max(lockMinutes * LoginAttemptConsts.SECONDS_PER_MINUTE,
                LoginAttemptConsts.FAIL_COUNT_MIN_TTL_SECONDS);
        String failKey = SystemRedisKeyConsts.LOGIN_FAIL_PREFIX + user;
        long count = redisCounter.incrWithTtl(failKey, failTtl);
        if (lockEnabled && count >= maxRetry) {
            redisClient.open(s -> {
                s.key(SystemRedisKeyConsts.LOGIN_LOCK_PREFIX + user).expire(lockMinutes * 60).set("1");
                s.key(failKey).delete();
            });
            throw new BizException("登录失败次数过多，账号已锁定 " + lockMinutes + " 分钟");
        }
        return count;
    }

    @Override
    public void removeLoginFail(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        String user = username.trim();
        redisClient.open(s -> {
            s.key(SystemRedisKeyConsts.LOGIN_FAIL_PREFIX + user).delete();
            s.key(SystemRedisKeyConsts.LOGIN_LOCK_PREFIX + user).delete();
        });
    }
}
