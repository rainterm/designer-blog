package com.designer.admin.module.blog.constant;

/**
 * 公开接口缓存的 tag 常量（Solon 声明式缓存）。
 *
 * <p>读侧在 {@code BlogPublicController} 用 {@code @Cache(tags=...)} 打标，
 * 写侧在各 service 的增删改方法上用 {@code @CacheRemove(tags=...)} 失效。
 */
public final class BlogCacheTags {

    /** 站点设置：{@code GET /app-api/blog/public/site} */
    public static final String SITE = "blog:public:site";

    /** 分类列表：{@code GET /app-api/blog/public/categories} */
    public static final String CATEGORY = "blog:public:category";

    /** 标签列表：{@code GET /app-api/blog/public/tags} */
    public static final String TAG = "blog:public:tag";

    /** 外链列表：{@code GET /app-api/blog/public/links} */
    public static final String LINK = "blog:public:link";

    private BlogCacheTags() {
    }
}
