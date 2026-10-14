package com.designer.admin.module.blog.service;

import com.designer.admin.common.exception.BizException;
import com.designer.admin.module.blog.model.vo.CommentCaptchaVO;
import com.designer.admin.module.blog.util.ClientIpUtil;
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
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 公开评论防刷网关：图形验证码 + 基于 IP 的频率限制。
 *
 * <p>背景：{@code /app-api/blog/public/comments} 是匿名接口，登录态、权限注解都用不上。
 * 若不设防，脚本可以无成本灌库（表膨胀 + 连接池耗尽）并把后台审核队列淹没。
 * 这里用 Redis 做无状态计数，不改动 system 模块的登录验证码（那是按账号维度设计的）。</p>
 *
 * <p>限流参数（需要调整时改这里的常量即可）：</p>
 * <ul>
 *   <li>同一 IP 两次提交最小间隔 {@value #MIN_INTERVAL_SECONDS} 秒</li>
 *   <li>同一 IP 24 小时内最多 {@value #DAILY_LIMIT} 条</li>
 *   <li>同一 IP 每分钟最多拉取 {@value #CAPTCHA_PER_MINUTE} 次验证码</li>
 * </ul>
 *
 * <p>注意：限流键基于客户端 IP，依赖反向代理正确覆写 {@code X-Forwarded-For}，
 * 否则 IP 可被伪造（详见 blog-web/README.md 的部署说明）。</p>
 */
@Component
public class BlogCommentGuard {

    private static final String CAPTCHA_KEY = "blog:comment:captcha:";
    private static final String CAPTCHA_GEN_KEY = "blog:comment:captcha:gen:";
    private static final String MIN_INTERVAL_KEY = "blog:comment:flood:min:";
    private static final String DAY_QUOTA_KEY = "blog:comment:flood:day:";

    /** 验证码有效期（秒） */
    private static final int CAPTCHA_TTL = 120;
    /** 两次提交的最小间隔（秒） */
    private static final int MIN_INTERVAL_SECONDS = 30;
    /** 同一 IP 24 小时内的提交上限 */
    private static final int DAILY_LIMIT = 20;
    private static final int DAY_SECONDS = 24 * 60 * 60;
    /** 同一 IP 每分钟的验证码拉取上限（防刷新接口被当资源消耗点） */
    private static final int CAPTCHA_PER_MINUTE = 20;
    /** 与 blog_comment.ip 列宽一致 */
    private static final int IP_MAX_LENGTH = 64;

    @Inject
    private RedisClient redisClient;

    /** 生成评论验证码（带拉取频率限制） */
    public CommentCaptchaVO createCaptcha() {
        assertCaptchaQuota(currentIp());

        String uuid = UUID.randomUUID().toString().replace("-", "");
        // [答案, 展示文本]
        String[] pair = buildMathCaptcha();
        redisClient.open0(s -> s.key(CAPTCHA_KEY + uuid).expire(CAPTCHA_TTL).set(pair[0]));

        CommentCaptchaVO vo = new CommentCaptchaVO();
        vo.setUuid(uuid);
        vo.setImg("data:image/png;base64," + renderImage(pair[1]));
        return vo;
    }

    /**
     * 提交前的准入校验：验证码 → 24h 配额 → 最小间隔。
     *
     * <p>顺序有讲究：验证码放最前后，后面的计数只会在「验证码正确」时发生，
     * 这样用户填错验证码不会白白消耗配额；而机器人因为过不了验证码，
     * 也就无法触碰配额计数。</p>
     *
     * @return 归一化后的客户端 IP，供落库使用
     */
    public String assertCanSubmit(String captchaUuid, String captchaCode) {
        String ip = currentIp();
        assertCaptcha(captchaUuid, captchaCode);
        assertDailyQuota(ip);
        assertMinInterval(ip);
        return ip;
    }

    /**
     * 归一化客户端 IP：取真实 IP 首段并截断。
     *
     * <p>与访问统计共用 {@link ClientIpUtil}，避免两处对 IP 的处理口径不一致
     * （同一次请求在评论表和访问日志里记成不同值最难排查）。</p>
     */
    public String currentIp() {
        return ClientIpUtil.current();
    }

    // ------------------------------------------------------------------
    // 校验细节
    // ------------------------------------------------------------------

    /** 校验并一次性消费验证码 */
    private void assertCaptcha(String uuid, String code) {
        if (uuid == null || uuid.isBlank() || code == null || code.isBlank()) {
            throw new BizException("请输入验证码");
        }
        String key = CAPTCHA_KEY + uuid.trim();
        String cached = redisClient.open1(s -> s.key(key).get());
        // 不论对错都消费，避免同一验证码被反复试探
        redisClient.open0(s -> s.key(key).delete());
        if (cached == null) {
            throw new BizException("验证码已过期，请刷新后重试");
        }
        if (!cached.equalsIgnoreCase(code.trim())) {
            throw new BizException("验证码错误");
        }
    }

    /** 24 小时滚动窗口内的提交上限 */
    private void assertDailyQuota(String ip) {
        long count = incrWithTtl(DAY_QUOTA_KEY + ip, DAY_SECONDS);
        if (count > DAILY_LIMIT) {
            throw new BizException("24 小时内评论条数已达上限，请稍后再试");
        }
    }

    /** 两次提交之间的最小间隔，挡住瞬间并发刷屏 */
    private void assertMinInterval(String ip) {
        long count = incrWithTtl(MIN_INTERVAL_KEY + ip, MIN_INTERVAL_SECONDS);
        if (count > 1) {
            throw new BizException("评论太频繁了，请 " + MIN_INTERVAL_SECONDS + " 秒后再试");
        }
    }

    /** 验证码拉取频率限制 */
    private void assertCaptchaQuota(String ip) {
        long count = incrWithTtl(CAPTCHA_GEN_KEY + ip, 60);
        if (count > CAPTCHA_PER_MINUTE) {
            throw new BizException("验证码请求过于频繁，请稍后再试");
        }
    }

    /** 计数 +1，首次写入时设置 TTL（滚动窗口） */
    private long incrWithTtl(String key, int ttlSeconds) {
        return redisClient.open1(s -> {
            long count = s.key(key).incr();
            if (count == 1) {
                s.key(key).expire(ttlSeconds);
            }
            return count;
        });
    }

    // ------------------------------------------------------------------
    // 图形验证码（算术题，与登录验证码同款实现）
    // ------------------------------------------------------------------

    /** @return [答案, 展示文本] */
    private String[] buildMathCaptcha() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        int a = r.nextInt(9) + 1;
        int b = r.nextInt(9) + 1;
        int answer;
        String text;
        switch (r.nextInt(3)) {
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
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < 6; i++) {
            g.setColor(new Color(180 + r.nextInt(60), 180 + r.nextInt(60), 180 + r.nextInt(60)));
            g.drawLine(r.nextInt(w), r.nextInt(h), r.nextInt(w), r.nextInt(h));
        }
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.setColor(new Color(22, 163, 74));
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
