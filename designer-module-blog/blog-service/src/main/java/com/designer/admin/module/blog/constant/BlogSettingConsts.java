package com.designer.admin.module.blog.constant;

import java.util.List;

/**
 * 站点设置（{@code blog_setting}，单行表）的默认值与上限。
 *
 * <p>默认值同时被「设置行缺失时的兜底读取」与「字段清空后的归一化回填」使用，
 * 两处必须完全一致，否则清空主题色后站点会换一个颜色。上限值则用于校验，
 * 与前端表单的 max 属性对齐。</p>
 *
 * @author 雨夜终端
 */
public final class BlogSettingConsts {

    /** 单行表的固定主键 */
    public static final long SINGLETON_ID = 1L;

    /** 默认主题色（与前台 theme.js 的 colors.primary 保持一致） */
    public static final String DEFAULT_THEME_COLOR = "#16A34A";

    /** 默认站长评论署名 */
    public static final String DEFAULT_OWNER_NICKNAME = "博主";

    /** Logo 圆角默认值（px） */
    public static final int DEFAULT_LOGO_RADIUS = 6;

    /** Logo 圆角上限（px），超过视为脏值 */
    public static final int MAX_LOGO_RADIUS = 100;

    /** 站长署名长度上限 */
    public static final int MAX_OWNER_NICKNAME_LENGTH = 50;

    /** 保留昵称配置的原始文本长度上限 */
    public static final int MAX_RESERVED_LENGTH = 500;

    /** 评论开关：开启 */
    public static final int COMMENT_ENABLED = 1;

    /** 内置的默认保留昵称（设置行缺失时的兜底，可被后台配置覆盖） */
    public static final List<String> DEFAULT_RESERVED_NICKNAMES = List.of("博主", "站长", "管理员", "admin");

    /** 默认保留昵称的存储形态，与 {@code blog_setting.reserved_nicknames} 的列默认值保持一致 */
    public static final String DEFAULT_RESERVED_NICKNAMES_TEXT = String.join(",", DEFAULT_RESERVED_NICKNAMES);

    private BlogSettingConsts() {
    }
}
