package com.designer.admin.system.service;

import com.designer.admin.system.constant.SystemCacheTags;
import com.designer.admin.system.mapper.SysSettingMapper;
import com.designer.admin.system.model.dto.SystemSettingSaveDTO;
import com.designer.admin.system.model.entity.SysSetting;
import com.designer.admin.system.model.vo.SystemSettingVO;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.data.annotation.CacheRemove;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * 系统设置（单行）：系统名称 / 登录页副标题 / Logo / 默认偏好设置。
 *
 * <p>与通用的参数配置（{@code sys_config} 键值对）不同，这里是后台「系统设置」菜单
 * 专属的结构化单行配置，固定 id=1。默认偏好用于「用户未在偏好设置面板自定义」时的兜底。</p>
 */
@Component
public class SysSettingService {

    /** 单行配置固定主键 */
    private static final long SINGLETON_ID = 1L;

    /** 系统名称默认值（与前端 settings.js 的 VITE_APP_TITLE 兜底保持一致） */
    public static final String DEFAULT_SYSTEM_NAME = "Designer Admin";

    /** 登录页副标题默认值（留空则不显示） */
    public static final String DEFAULT_LOGIN_SUBTITLE = "统一管理后台，让协作更高效";

    /** Logo 默认圆角（px） */
    private static final int DEFAULT_LOGO_RADIUS = 6;

    /** Logo 圆角上限（px），避免异常值破坏布局 */
    private static final int MAX_LOGO_RADIUS = 100;

    /** 默认布局模式（与前端 constants/layout.js 的 DEFAULT_LAYOUT_MODE 保持一致） */
    public static final String DEFAULT_LAYOUT_MODE = "mixed-vertical";

    /** 默认主题色（与前端 theme/index.js 的 DEFAULT_PRIMARY 保持一致） */
    public static final String DEFAULT_PRIMARY_COLOR = "#16A34A";

    /** 合法布局模式白名单（与前端 constants/layout.js 的 LAYOUT_OPTIONS 保持一致） */
    private static final Set<String> LAYOUT_MODES = Set.of(
            "vertical", "dual-column", "horizontal", "side-nav",
            "mixed-vertical", "mixed-dual", "content-full");

    /** 主题色：仅接受 #RRGGBB */
    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9a-fA-F]{6}$");

    @Db
    private SysSettingMapper settingMapper;

    public SysSetting get() {
        SysSetting setting = settingMapper.selectById(SINGLETON_ID);
        return setting == null ? empty() : setting;
    }

    /**
     * 公开视图：仅暴露前台（登录页 / 布局）需要的字段，并做归一化兜底。
     *
     * <p>读缓存由 {@code SysSettingController#publicSetting} 的 {@code @Cache} 承担，
     * 本方法自身不缓存。</p>
     */
    public SystemSettingVO getPublic() {
        SysSetting s = get();
        SystemSettingVO vo = new SystemSettingVO();
        vo.setSystemName(normalizeName(s.getSystemName()));
        vo.setLoginSubtitle(trim(s.getLoginSubtitle()));
        vo.setLogoUrl(trim(s.getLogoUrl()));
        vo.setLogoRadius(normalizeRadius(s.getLogoRadius()));
        vo.setDefaultLayoutMode(normalizeLayoutMode(s.getDefaultLayoutMode()));
        vo.setDefaultPrimaryColor(normalizeColor(s.getDefaultPrimaryColor()));
        return vo;
    }

    /** 保存（单行：存在则更新，不存在则创建） */
    @CacheRemove(tags = SystemCacheTags.SETTING_PUBLIC)
    public void update(SystemSettingSaveDTO dto) {
        SysSetting entity = new SysSetting();
        entity.setId(SINGLETON_ID);
        entity.setSystemName(normalizeName(dto.getSystemName()));
        entity.setLoginSubtitle(trim(dto.getLoginSubtitle()));
        entity.setLogoUrl(trim(dto.getLogoUrl()));
        entity.setLogoRadius(normalizeRadius(dto.getLogoRadius()));
        entity.setDefaultLayoutMode(normalizeLayoutMode(dto.getDefaultLayoutMode()));
        entity.setDefaultPrimaryColor(normalizeColor(dto.getDefaultPrimaryColor()));

        if (settingMapper.selectById(SINGLETON_ID) == null) {
            settingMapper.insert(entity);
        } else {
            settingMapper.updateById(entity);
        }
        // 缓存失效由方法上的 @CacheRemove 在返回后执行
    }

    private SysSetting empty() {
        SysSetting s = new SysSetting();
        s.setId(SINGLETON_ID);
        s.setSystemName(DEFAULT_SYSTEM_NAME);
        s.setLoginSubtitle(DEFAULT_LOGIN_SUBTITLE);
        s.setLogoUrl("");
        s.setLogoRadius(DEFAULT_LOGO_RADIUS);
        s.setDefaultLayoutMode(DEFAULT_LAYOUT_MODE);
        s.setDefaultPrimaryColor(DEFAULT_PRIMARY_COLOR);
        return s;
    }

    /** 系统名称归一化：空白回默认值，避免登录页标题变成空白 */
    private String normalizeName(String raw) {
        String name = trim(raw);
        return name.isEmpty() ? DEFAULT_SYSTEM_NAME : name;
    }

    /** 圆角兜底与裁剪：非法回默认，0 为合法值（直角） */
    private int normalizeRadius(Integer radius) {
        if (radius == null || radius < 0) {
            return DEFAULT_LOGO_RADIUS;
        }
        return Math.min(radius, MAX_LOGO_RADIUS);
    }

    /** 布局模式：白名单外一律回默认，避免脏值让前端布局失效 */
    private String normalizeLayoutMode(String mode) {
        String value = trim(mode);
        return LAYOUT_MODES.contains(value) ? value : DEFAULT_LAYOUT_MODE;
    }

    /** 主题色：仅接受 #RRGGBB，非法回默认 */
    private String normalizeColor(String color) {
        String value = trim(color);
        return HEX_COLOR.matcher(value).matches() ? value.toUpperCase() : DEFAULT_PRIMARY_COLOR;
    }

    private String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
