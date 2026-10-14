package com.designer.admin.module.blog.util;

import java.net.URI;
import java.util.List;

/**
 * 从 Referer 解析来源域名与来源类型。
 *
 * <p>关键点：**站内跳转不算「来源」**。博客页面之间的互相跳转（含带域名与不带域名的写法）
 * 都会带上当前站点自身作为 Referer，若照单全收，「来源 Top」会被自家域名霸榜，
 * 外链与搜索引擎流量反而被淹没。因此与站点自身域名一致时归为直接访问。</p>
 */
public final class RefererUtil {

    public static final String DIRECT = "direct";
    public static final String SEARCH = "search";
    public static final String EXTERNAL = "external";

    /** 与 blog_visit_log.referer_host 列宽一致 */
    private static final int HOST_MAX_LENGTH = 128;

    /** 常见搜索引擎域名特征（含子域名，故用 contains 匹配） */
    private static final List<String> SEARCH_ENGINES = List.of(
            "baidu.com", "google.", "bing.com", "sogou.com", "so.com", "sm.cn",
            "yandex.", "duckduckgo.com", "yahoo.com", "ecosia.", "naver.com",
            "daum.net", "qwant.com", "brave.com", "startpage.com");

    private RefererUtil() {
    }

    /**
     * 解析来源域名。
     *
     * @param referer  请求头 Referer；为空表示直接访问
     * @param selfHost 当前站点 host（含端口亦可），用于识别站内跳转
     * @return 归一化域名；直接访问或站内跳转返回空串
     */
    public static String host(String referer, String selfHost) {
        String uriHost = extractHost(referer);
        if (uriHost.isEmpty()) {
            return "";
        }
        if (uriHost.equals(normalizeHost(selfHost))) {
            return "";
        }
        return uriHost.length() > HOST_MAX_LENGTH ? uriHost.substring(0, HOST_MAX_LENGTH) : uriHost;
    }

    /** 按来源域名判断类型；空域名代表直接访问 */
    public static String type(String host) {
        if (host == null || host.isEmpty()) {
            return DIRECT;
        }
        String lower = host.toLowerCase();
        for (String engine : SEARCH_ENGINES) {
            if (lower.contains(engine)) {
                return SEARCH;
            }
        }
        return EXTERNAL;
    }

    /** 提取并归一化域名；无法解析时返回空串（不抛异常，埋点不能因脏 Referer 失败） */
    private static String extractHost(String referer) {
        if (referer == null || referer.isBlank()) {
            return "";
        }
        try {
            URI uri = URI.create(referer.trim());
            String host = uri.getHost();
            return host == null ? "" : normalizeHost(host);
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    /** 统一小写并去掉 www. 前缀，让 www.example.com 与 example.com 视为同一站点 */
    private static String normalizeHost(String host) {
        if (host == null) {
            return "";
        }
        String value = host.trim().toLowerCase();
        int colon = value.indexOf(':');
        if (colon > 0) {
            value = value.substring(0, colon);
        }
        return value.startsWith("www.") ? value.substring(4) : value;
    }
}
