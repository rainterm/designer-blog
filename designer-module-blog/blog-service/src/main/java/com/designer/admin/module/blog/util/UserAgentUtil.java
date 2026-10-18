package com.designer.admin.module.blog.util;

/**
 * 把 User-Agent 归类成设备类型。
 *
 * <p>判定顺序有讲究：先平板再移动，因为部分平板 UA 同时含 {@code Android} 与 {@code Mobile}；
 * 浏览器自称的「平板模式」请求桌面版 UA 时会落回 PC，这是业界通行做法，不必强求准确。</p>
 *
 * <p>爬虫单列为 {@code BOT} 而不是并入 {@code OTHER}：个人站点的流量里机器人占比往往不低，
 * 明确标出来才好判断「访问量涨了」到底是真人还是抓取。</p>
 *
 * @author 雨夜终端
 */
public final class UserAgentUtil {

    public static final String PC = "PC";
    public static final String MOBILE = "MOBILE";
    public static final String TABLET = "TABLET";
    public static final String BOT = "BOT";
    public static final String OTHER = "OTHER";

    /** 与 blog_visit_log.ua 列宽一致 */
    private static final int UA_MAX_LENGTH = 255;

    /** 平板关键字（小写） */
    private static final String[] TABLET_KEYS = {"ipad", "tablet", "kindle", "silk"};
    /** 手机关键字（小写） */
    private static final String[] MOBILE_KEYS = {
            "mobi", "iphone", "ipod", "android", "windows phone", "blackberry", "opera mini"};
    /** 桌面关键字（小写） */
    private static final String[] PC_KEYS = {"windows", "macintosh", "linux", "x11", "cros"};
    /** 爬虫关键字（小写） */
    private static final String[] BOT_KEYS = {
            "bot", "spider", "crawler", "spdy", "slurp", "curl",
            "wget", "python-requests", "headless", "lighthouse"};
    /** Android 标识 */
    private static final String UA_ANDROID = "android";
    /** 移动端标识 */
    private static final String UA_MOBILE = "mobile";

    private UserAgentUtil() {
    }

    /**
     * 归类设备类型；缺失或无法识别时返回 {@code OTHER}。
     *
     * @param ua 原始 User-Agent
     * @return 设备类型
     */
    public static String device(String ua) {
        if (ua == null || ua.isBlank()) {
            return OTHER;
        }
        String s = ua.toLowerCase();

        if (containsAny(s, BOT_KEYS)) {
            return BOT;
        }
        // 平板优先于手机：iPad / Android 平板 UA 里也可能出现 mobile 关键字
        boolean androidTablet = s.contains(UA_ANDROID) && !s.contains(UA_MOBILE);
        if (containsAny(s, TABLET_KEYS) || androidTablet) {
            return TABLET;
        }
        if (containsAny(s, MOBILE_KEYS)) {
            return MOBILE;
        }
        if (containsAny(s, PC_KEYS)) {
            return PC;
        }
        return OTHER;
    }

    /**
     * 截断到与库列一致的长度。
     *
     * @param ua 原始 User-Agent
     * @return 截断后的 User-Agent
     */
    public static String truncate(String ua) {
        if (ua == null) {
            return "";
        }
        String value = ua.trim();
        return value.length() > UA_MAX_LENGTH ? value.substring(0, UA_MAX_LENGTH) : value;
    }

    /**
     * 判断文本是否包含任意一个关键字。
     *
     * @param text 待匹配文本
     * @param keys 关键字数组
     * @return 命中任意关键字时返回 {@code true}
     */
    private static boolean containsAny(String text, String[] keys) {
        for (String key : keys) {
            if (text.contains(key)) {
                return true;
            }
        }
        return false;
    }
}
