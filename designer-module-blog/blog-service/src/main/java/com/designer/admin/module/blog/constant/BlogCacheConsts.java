package com.designer.admin.module.blog.constant;

/**
 * 公开接口缓存的常量（Solon 声明式缓存）：tag 与兜底存活时间。
 *
 * <p>读侧在 {@code BlogPublicController} 用 {@code @Cache(tags=...)} 打标，
 * 写侧在各 service 的增删改方法上用 {@code @CacheRemove(tags=...)} 失效。
 *
 * @author 雨夜终端
 */
public final class BlogCacheConsts {

    /** 公开接口缓存的兜底存活时间（秒）：1 小时 */
    public static final int CACHE_SECONDS = 3600;

    /** 站点设置：{@code GET /app-api/blog/public/site} */
    public static final String SITE = "blog:public:site";

    /** 分类列表：{@code GET /app-api/blog/public/categories} */
    public static final String CATEGORY = "blog:public:category";

    /** 标签列表：{@code GET /app-api/blog/public/tags} */
    public static final String TAG = "blog:public:tag";

    /** 外链列表：{@code GET /app-api/blog/public/links} */
    public static final String LINK = "blog:public:link";

    private BlogCacheConsts() {
    }
}
