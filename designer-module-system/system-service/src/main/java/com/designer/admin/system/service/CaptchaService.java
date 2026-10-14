package com.designer.admin.system.service;

import com.designer.admin.common.exception.BizException;
import com.designer.admin.system.model.vo.CaptchaVO;
import org.noear.redisx.RedisClient;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 图形验证码（算术题）+ 登录失败锁定。
 * 验证码仅在「全局开启」且「该账号连续失败达到阈值」后要求填写。
 */
@Component
public class CaptchaService {

    private static final String CAPTCHA_KEY = "login:captcha:";
    private static final String FAIL_KEY = "login:fail:";
    private static final String LOCK_KEY = "login:lock:";
    private static final int CAPTCHA_TTL = 120;
    /** 连续失败达到该次数后才要求验证码（默认 3） */
    public static final int DEFAULT_CAPTCHA_FAIL_THRESHOLD = 3;

    @Inject
    private RedisClient redisClient;
    @Inject
    private SysConfigService configService;

    /**
     * 按账号生成验证码。未达失败阈值或全局关闭时 enabled=false。
     */
    public CaptchaVO createCaptcha(String username) {
        CaptchaVO vo = new CaptchaVO();
        if (!needCaptcha(username)) {
            vo.setEnabled(false);
            return vo;
        }
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String[] pair = buildMathCaptcha();
        String code = pair[0];
        String text = pair[1];
        redisClient.open0(s -> s.key(CAPTCHA_KEY + uuid).expire(CAPTCHA_TTL).set(code));
        vo.setEnabled(true);
        vo.setUuid(uuid);
        vo.setImg("data:image/png;base64," + renderImage(text));
        return vo;
    }

    /** 是否需要校验验证码：全局开启且该账号失败次数 ≥ 阈值 */
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

    /** 校验验证码（达到失败阈值时必填且一次性消费） */
    public void validateCaptcha(String username, String uuid, String captcha) {
        if (!needCaptcha(username)) {
            return;
        }
        if (uuid == null || uuid.isBlank() || captcha == null || captcha.isBlank()) {
            throw new BizException("请输入验证码");
        }
        String key = CAPTCHA_KEY + uuid;
        String cached = redisClient.open1(s -> s.key(key).get());
        redisClient.open0(s -> s.key(key).delete());
        if (cached == null) {
            throw new BizException("验证码已过期");
        }
        if (!cached.equalsIgnoreCase(captcha.trim())) {
            throw new BizException("验证码错误");
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
        long count = redisClient.open1(s -> {
            long c = s.key(failKey).incr();
            if (c == 1) {
                s.key(failKey).expire(failTtl);
            }
            return c;
        });
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

    /** @return [答案, 展示文本] */
    private String[] buildMathCaptcha() {
        Random r = ThreadLocalRandom.current();
        int a = r.nextInt(9) + 1;
        int b = r.nextInt(9) + 1;
        int op = r.nextInt(3);
        int answer;
        String text;
        switch (op) {
            case 0 -> {
                answer = a + b;
                text = a + "+" + b + "=?";
            }
            case 1 -> {
                if (a < b) {
                    int t = a;
                    a = b;
                    b = t;
                }
                answer = a - b;
                text = a + "-" + b + "=?";
            }
            default -> {
                answer = a * b;
                text = a + "×" + b + "=?";
            }
        }
        return new String[]{String.valueOf(answer), text};
    }

    private String renderImage(String text) {
        int w = 120;
        int h = 40;
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(245, 247, 250));
        g.fillRect(0, 0, w, h);
        Random r = ThreadLocalRandom.current();
        for (int i = 0; i < 6; i++) {
            g.setColor(new Color(180 + r.nextInt(60), 180 + r.nextInt(60), 180 + r.nextInt(60)));
            g.drawLine(r.nextInt(w), r.nextInt(h), r.nextInt(w), r.nextInt(h));
        }
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.setColor(new Color(37, 99, 235));
        g.drawString(text, 18, 28);
        g.dispose();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            throw new BizException("生成验证码失败");
        }
    }
}
