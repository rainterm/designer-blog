package com.designer.admin.module.blog.service;

import com.designer.admin.module.blog.mapper.BlogSettingMapper;
import com.designer.admin.module.blog.model.dto.SettingSaveDTO;
import com.designer.admin.module.blog.model.entity.BlogSetting;
import com.designer.admin.module.blog.model.vo.PublicSiteVO;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;

/**
 * 博客站点设置（单行）：站点名称 / Logo / 页脚版权 / 备案号
 */
@Component
public class BlogSettingService {

    /** 单行配置固定主键 */
    private static final long SINGLETON_ID = 1L;

    /** Logo 默认圆角（px） */
    private static final int DEFAULT_LOGO_RADIUS = 6;

    /** Logo 圆角上限（px），避免异常值破坏布局 */
    private static final int MAX_LOGO_RADIUS = 100;

    /** 评论总开关默认值：缺省 / 入库前默认开启 */
    private static final int COMMENT_ENABLED = 1;

    @Db
    private BlogSettingMapper settingMapper;

    /** 取设置；未初始化时返回空值对象，由前端回退内置默认 */
    public BlogSetting get() {
        BlogSetting setting = settingMapper.selectById(SINGLETON_ID);
        return setting == null ? empty() : setting;
    }

    public PublicSiteVO getPublic() {
        BlogSetting s = get();
        PublicSiteVO vo = new PublicSiteVO();
        vo.setSiteName(s.getSiteName());
        vo.setSiteSubtitle(s.getSiteSubtitle());
        vo.setLogoUrl(s.getLogoUrl());
        vo.setLogoRadius(normalizeRadius(s.getLogoRadius()));
        vo.setCopyright(s.getCopyright());
        vo.setIcp(s.getIcp());
        vo.setIcpUrl(normalizeLink(s.getIcpUrl()));
        vo.setSeoKeywords(s.getSeoKeywords());
        vo.setSeoDescription(s.getSeoDescription());
        vo.setCommentEnabled(normalizeEnabled(s.getCommentEnabled()));
        return vo;
    }

    /**
     * 全局评论是否开启。
     *
     * <p>给评论模块判断用：设置行缺失或是脏值时按**开启**处理，
     * 避免因为一条配置没写好就静默把整站评论关掉。</p>
     */
    public boolean isCommentEnabled() {
        return normalizeEnabled(get().getCommentEnabled()) == COMMENT_ENABLED;
    }

    /** 保存（单行：存在则更新，不存在则创建） */
    public void update(SettingSaveDTO dto) {
        BlogSetting entity = new BlogSetting();
        entity.setId(SINGLETON_ID);
        entity.setSiteName(trim(dto.getSiteName()));
        entity.setSiteSubtitle(trim(dto.getSiteSubtitle()));
        entity.setLogoUrl(trim(dto.getLogoUrl()));
        entity.setLogoRadius(normalizeRadius(dto.getLogoRadius()));
        entity.setCopyright(trim(dto.getCopyright()));
        entity.setIcp(trim(dto.getIcp()));
        entity.setIcpUrl(normalizeLink(dto.getIcpUrl()));
        entity.setSeoKeywords(trim(dto.getSeoKeywords()));
        entity.setSeoDescription(trim(dto.getSeoDescription()));
        entity.setCommentEnabled(normalizeEnabled(dto.getCommentEnabled()));

        if (settingMapper.selectById(SINGLETON_ID) == null) {
            settingMapper.insert(entity);
        } else {
            settingMapper.updateById(entity);
        }
    }

    private BlogSetting empty() {
        BlogSetting s = new BlogSetting();
        s.setId(SINGLETON_ID);
        s.setSiteName("");
        s.setSiteSubtitle("");
        s.setLogoUrl("");
        s.setLogoRadius(DEFAULT_LOGO_RADIUS);
        s.setCopyright("");
        s.setIcp("");
        s.setIcpUrl("");
        s.setSeoKeywords("");
        s.setSeoDescription("");
        s.setCommentEnabled(COMMENT_ENABLED);
        return s;
    }

    /** 链接补全协议：填 beian.miit.gov.cn 也能跳转 */
    private String normalizeLink(String url) {
        String value = trim(url);
        if (value.isEmpty()) {
            return "";
        }
        String lower = value.toLowerCase();
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return value;
        }
        return "https://" + value;
    }

    /** 圆角兜底与裁剪：非法回默认，0 为合法值（直角） */
    private int normalizeRadius(Integer radius) {
        if (radius == null || radius < 0) {
            return DEFAULT_LOGO_RADIUS;
        }
        return Math.min(radius, MAX_LOGO_RADIUS);
    }

    /** 开关归一化：只认 0 为关闭，其余（含 null）按开启处理 */
    private int normalizeEnabled(Integer enabled) {
        return (enabled != null && enabled == 0) ? 0 : COMMENT_ENABLED;
    }

    private String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
