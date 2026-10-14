package com.designer.admin.module.blog.util;

import lombok.Getter;

/**
 * 把访问路径归类成页面类型，并提取 slug。
 *
 * <p>路由规则与 blog-web 的 {@code router/index.js} 一一对应，
 * 两边改动时要同步，否则统计会把页面记成 {@code other}。</p>
 */
public final class VisitPathUtil {

    /** 与 blog_visit_log.path 列宽一致 */
    private static final int PATH_MAX_LENGTH = 255;

    private VisitPathUtil() {
    }

    /** 页面类型：与 blog-web 路由一一对应 */
    public enum PageType {
        /** 首页即文章列表（blog-web 的 `''` 路由） */
        HOME("home"),
        POST("post"),
        TAG("tag"),
        CATEGORY("category"),
        OTHER("other");

        @Getter
        private final String code;

        PageType(String code) {
            this.code = code;
        }
    }

    /** 归类结果 */
    public record Parsed(PageType type, String path, String slug) {
    }

    /**
     * 解析路径。
     *
     * <p>先剥掉 query 与锚点：埋点只关心「看了哪个页面」，
     * 把 {@code ?pageNum=2} 之类记进去只会让 path 变脏、让去重失效。</p>
     *
     * <p>接口路径（{@code /app-api/**}、{@code /admin-api/**}）不是页面，
     * 一律按无效处理并返回空 path，由调用方跳过——埋点宁可少记，也不该记错。</p>
     */
    public static Parsed parse(String rawPath) {
        String path = normalize(rawPath);
        if (path.isEmpty()) {
            return new Parsed(PageType.OTHER, "", "");
        }
        if (isApiPath(path)) {
            return new Parsed(PageType.OTHER, "", "");
        }
        if (path.equals("/")) {
            return new Parsed(PageType.HOME, path, "");
        }
        String[] prefixes = {"/post/", "/tag/", "/category/"};
        PageType[] types = {PageType.POST, PageType.TAG, PageType.CATEGORY};
        for (int i = 0; i < prefixes.length; i++) {
            if (path.startsWith(prefixes[i])) {
                String slug = firstSegment(path.substring(prefixes[i].length()));
                // 缺少 slug 的路径（如 /post/）不算有效页面，避免污染文章统计
                if (slug.isEmpty()) {
                    break;
                }
                return new Parsed(types[i], path, slug);
            }
        }
        return new Parsed(PageType.OTHER, path, "");
    }

    /** 去 query / 锚点，补前导斜杠，去掉尾部斜杠，截断到列宽 */
    public static String normalize(String rawPath) {
        if (rawPath == null) {
            return "";
        }
        String path = rawPath.trim();
        int hash = path.indexOf('#');
        if (hash >= 0) {
            path = path.substring(0, hash);
        }
        int query = path.indexOf('?');
        if (query >= 0) {
            path = path.substring(0, query);
        }
        if (path.isEmpty()) {
            return "";
        }
        if (path.charAt(0) != '/') {
            path = "/" + path;
        }
        // 去掉尾部斜杠，让 /tag/java/ 与 /tag/java 视为同一页（否则去重会各算一次）
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path.length() > PATH_MAX_LENGTH ? path.substring(0, PATH_MAX_LENGTH) : path;
    }

    /** 取下一段路径，遇到斜杠即停 */
    private static String firstSegment(String rest) {
        int slash = rest.indexOf('/');
        return (slash >= 0 ? rest.substring(0, slash) : rest).trim();
    }

    /** 接口路径不是页面，不能当成访问记录 */
    private static boolean isApiPath(String path) {
        return path.startsWith("/app-api") || path.startsWith("/admin-api");
    }
}
