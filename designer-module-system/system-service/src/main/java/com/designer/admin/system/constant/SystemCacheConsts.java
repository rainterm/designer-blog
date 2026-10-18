package com.designer.admin.system.constant;

/**
 * 公开接口缓存的常量（Solon 声明式缓存）：tag 与兜底存活时间。
 *
 * <p>读侧在 {@code SysSettingController#getPublicSetting} 用 {@code @Cache(tags=...)} 打标，
 * 写侧在 {@code SysSettingService#update} 用 {@code @CacheRemove(tags=...)} 失效。</p>
 *
 * @author 雨夜终端
 */
public final class SystemCacheConsts {

    /** 公开接口缓存的兜底存活时间（秒）：1 小时 */
    public static final int CACHE_SECONDS = 3600;

    /** 公开系统设置：{@code GET /admin-api/system/setting/public} */
    public static final String SETTING_PUBLIC = "system:public:setting";

}
