package com.designer.admin.system.constant;

/**
 * 公开接口缓存的 tag 常量（Solon 声明式缓存）。
 *
 * <p>读侧在 {@code SysSettingController#publicSetting} 用 {@code @Cache(tags=...)} 打标，
 * 写侧在 {@code SysSettingService#update} 用 {@code @CacheRemove(tags=...)} 失效。</p>
 */
public final class SystemCacheTags {

    /** 公开系统设置：{@code GET /admin-api/system/setting/public} */
    public static final String SETTING_PUBLIC = "system:public:setting";

    private SystemCacheTags() {
    }
}
