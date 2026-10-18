package com.designer.admin.module.blog.util;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * URL slug 生成：保留 a-z0-9-；空则用时间戳/UUID 兜底
 *
 * @author 雨夜终端
 */
public final class SlugUtil {

    /** slug 中的连接符 */
    private static final String DASH = "-";
    /** slug 长度上限，与 blog_post.slug 列宽一致（VARCHAR(200)）的一半 */
    private static final int MAX_SLUG_LENGTH = 100;
    private static final Pattern NON_SLUG = Pattern.compile("[^a-z0-9\\-]+");
    private static final Pattern MULTI_DASH = Pattern.compile("-{2,}");

    private SlugUtil() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String s = raw.trim().toLowerCase(Locale.ROOT).replace(' ', '-');
        s = NON_SLUG.matcher(s).replaceAll(DASH);
        s = MULTI_DASH.matcher(s).replaceAll(DASH);
        if (s.startsWith(DASH)) {
            s = s.substring(1);
        }
        if (s.endsWith(DASH)) {
            s = s.substring(0, s.length() - 1);
        }
        return s.length() > MAX_SLUG_LENGTH ? s.substring(0, MAX_SLUG_LENGTH) : s;
    }

    /** 优先使用传入 slug，其次从 title 规范化，最后时间戳兜底 */
    public static String resolve(String slug, String title) {
        String s = normalize(slug);
        if (!s.isEmpty()) {
            return s;
        }
        s = normalize(title);
        if (!s.isEmpty()) {
            return s;
        }
        return "post-" + System.currentTimeMillis();
    }

    public static String uniqueFallback() {
        return "item-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
