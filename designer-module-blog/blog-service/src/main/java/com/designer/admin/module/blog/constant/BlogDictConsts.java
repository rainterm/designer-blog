package com.designer.admin.module.blog.constant;

/**
 * 博客模块用到的数据字典类型标识。
 *
 * <p>库里的访问记录只存与语言无关的码（{@code PC / direct / post}），
 * 中文文案统一放在 {@code sys_dict_data} 里，由后台「系统管理 → 数据字典」维护：
 * 运营想改「电脑」为「桌面端」不必发版，新增设备类型也只是加一条字典项。</p>
 *
 * <p><b>标识是契约</b>：这里的值必须与 {@code sql/init/mysql/blog/data.sql}
 * 以及升级脚本里的 {@code dict_type} 完全一致，改一处就要同步改种子 SQL，
 * 否则线上表现为「码翻不出文案，列表退化成码值」。</p>
 *
 * @author 雨夜终端
 */
public final class BlogDictConsts {

    /** 访问记录的页面类型：home / post / tag / category / other */
    public static final String VISIT_PAGE_TYPE = "blog_visit_page_type";

    /** 访问记录的设备类型：PC / MOBILE / TABLET / BOT / OTHER */
    public static final String VISIT_DEVICE = "blog_visit_device";

    /** 访问记录的来源类型：direct / search / external */
    public static final String VISIT_REFERER_TYPE = "blog_visit_referer_type";

    private BlogDictConsts() {
    }
}
