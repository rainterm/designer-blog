package com.designer.admin.module.blog.util;

/**
 * 把 User-Agent 归类成设备类型。
 *
 * <p>判定顺序有讲究：先平板再移动，因为部分平板 UA 同时含 {@code Android} 与 {@code Mobile}；
 * 浏览器自称的「平板模式」请求桌面版 UA 时会落回 PC，这是业界通行做法，不必强求准确。</p>
 *
 * <p>爬虫单列为 {@code BOT} 而不是并入 {@code OTHER}：个人站点的流量里机器人占比往往不低，
 * 明确标出来才好判断「访问量涨了」到底是真人还是抓取。</p>
 */
public final class UserAgentUtil {

    public static final String PC = "PC";
    public static final String MOBILE = "MOBILE";
    public static final String TABLET = "TABLET";
    public static final String BOT = "BOT";
    public static final String OTHER = "OTHER";

    /** 与 blog_visit_log.ua 列宽一致 */
    private static final int UA_MAX_LENGTH = 255;

    private UserAgentUtil() {
    }

    /** 归类设备类型；缺失或无法识别时返回 {@code OTHER} */
    public static String device(String ua) {
        if (ua == null || ua.isBlank()) {
            return OTHER;
        }
        String s = ua.toLowerCase();

        if (isBot(s)) {
            return BOT;
        }
        // 平板优先于手机：iPad / Android 平板 UA 里也可能出现 mobile 关键字
        if (s.contains("ipad") || s.contains("tablet") || s.contains("kindle") || s.contains("silk")
                || (s.contains("android") && !s.contains("mobile"))) {
            return TABLET;
        }
        if (s.contains("mobi") || s.contains("iphone") || s.contains("ipod") || s.contains("android")
                || s.contains("windows phone") || s.contains("blackberry") || s.contains("opera mini")) {
            return MOBILE;
        }
        if (s.contains("windows") || s.contains("macintosh") || s.contains("linux")
                || s.contains("x11") || s.contains("cros")) {
            return PC;
        }
        return OTHER;
    }

    /** 截断到与库列一致的长度 */
    public static String truncate(String ua) {
        if (ua == null) {
            return "";
        }
        String value = ua.trim();
        return value.length() > UA_MAX_LENGTH ? value.substring(0, UA_MAX_LENGTH) : value;
    }

    private static boolean isBot(String lowerUa) {
        return lowerUa.contains("bot") || lowerUa.contains("spider") || lowerUa.contains("crawler")
                || lowerUa.contains("spdy") || lowerUa.contains("slurp") || lowerUa.contains("curl")
                || lowerUa.contains("wget") || lowerUa.contains("python-requests")
                || lowerUa.contains("headless") || lowerUa.contains("lighthouse");
    }
}
