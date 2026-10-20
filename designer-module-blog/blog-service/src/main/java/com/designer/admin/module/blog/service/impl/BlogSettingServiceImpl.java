package com.designer.admin.module.blog.service.impl;

import com.designer.admin.common.exception.BizException;
import com.designer.admin.common.constant.HttpConsts;
import com.designer.admin.module.blog.constant.BlogCacheConsts;
import com.designer.admin.module.blog.constant.BlogSettingConsts;
import com.designer.admin.module.blog.mapper.BlogSettingMapper;
import com.designer.admin.module.blog.model.dto.SettingSaveDTO;
import com.designer.admin.module.blog.model.entity.BlogSetting;
import com.designer.admin.module.blog.model.vo.PublicSiteVO;
import com.designer.admin.module.blog.service.BlogCommentService;
import com.designer.admin.module.blog.service.BlogSettingService;
import com.designer.admin.module.blog.util.NicknameUtil;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.data.annotation.CacheRemove;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 博客站点设置（单行）：站点名称 / Logo / 主题色 / 页脚版权 / 备案号
 *
 * @author 雨夜终端
 */
@Component
public class BlogSettingServiceImpl implements BlogSettingService {

    /** 主题色：仅接受 #RRGGBB */
    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9a-fA-F]{6}$");

    @Db
    private BlogSettingMapper settingMapper;

    @Override
    public BlogSetting getSetting() {
        BlogSetting setting = settingMapper.selectById(BlogSettingConsts.SINGLETON_ID);
        return setting == null ? empty() : setting;
    }

    @Override
    public PublicSiteVO getPublic() {
        BlogSetting s = getSetting();
        PublicSiteVO vo = new PublicSiteVO();
        vo.setSiteName(s.getSiteName());
        vo.setSiteSubtitle(s.getSiteSubtitle());
        vo.setLogoUrl(s.getLogoUrl());
        vo.setLogoRadius(normalizeRadius(s.getLogoRadius()));
        vo.setThemeColor(normalizeColor(s.getThemeColor()));
        vo.setCopyright(s.getCopyright());
        vo.setIcp(s.getIcp());
        vo.setIcpUrl(normalizeLink(s.getIcpUrl()));
        vo.setPoliceIcp(s.getPoliceIcp());
        vo.setPoliceIcpUrl(normalizeLink(s.getPoliceIcpUrl()));
        vo.setSeoKeywords(s.getSeoKeywords());
        vo.setSeoDescription(s.getSeoDescription());
        vo.setCommentEnabled(normalizeEnabled(s.getCommentEnabled()));
        String owner = ownerNickname(s.getOwnerNickname());
        vo.setOwnerNickname(owner);
        // 名单与拦截同源：BlogCommentService 提交校验时也走 NicknameUtil.reserved，
        // 所以前台拿到的名单不会与后端实际规则脱节
        vo.setReservedNicknames(NicknameUtil.reserved(s.getReservedNicknames(), owner));
        return vo;
    }

    @Override
    public String getOwnerNickname() {
        return ownerNickname(getSetting().getOwnerNickname());
    }

    @Override
    public List<String> listReservedNicknames() {
        BlogSetting s = getSetting();
        return NicknameUtil.reserved(s.getReservedNicknames(), ownerNickname(s.getOwnerNickname()));
    }

    @Override
    public boolean isCommentEnabled() {
        return normalizeEnabled(getSetting().getCommentEnabled()) == BlogSettingConsts.COMMENT_ENABLED;
    }

    @Override
    @CacheRemove(tags = BlogCacheConsts.SITE)
    public void update(SettingSaveDTO dto) {
        BlogSetting entity = new BlogSetting();
        entity.setId(BlogSettingConsts.SINGLETON_ID);
        entity.setSiteName(trim(dto.getSiteName()));
        entity.setSiteSubtitle(trim(dto.getSiteSubtitle()));
        entity.setLogoUrl(trim(dto.getLogoUrl()));
        entity.setLogoRadius(normalizeRadius(dto.getLogoRadius()));
        entity.setThemeColor(normalizeColor(dto.getThemeColor()));
        entity.setCopyright(trim(dto.getCopyright()));
        entity.setIcp(trim(dto.getIcp()));
        entity.setIcpUrl(normalizeLink(dto.getIcpUrl()));
        entity.setPoliceIcp(trim(dto.getPoliceIcp()));
        entity.setPoliceIcpUrl(normalizeLink(dto.getPoliceIcpUrl()));
        entity.setSeoKeywords(trim(dto.getSeoKeywords()));
        entity.setSeoDescription(trim(dto.getSeoDescription()));
        entity.setCommentEnabled(normalizeEnabled(dto.getCommentEnabled()));
        entity.setOwnerNickname(ownerNickname(dto.getOwnerNickname()));
        entity.setReservedNicknames(normalizeReserved(dto.getReservedNicknames()));

        if (settingMapper.selectById(BlogSettingConsts.SINGLETON_ID) == null) {
            settingMapper.insert(entity);
        } else {
            settingMapper.updateById(entity);
        }
        // 失效由方法上的 @CacheRemove 在方法返回后执行，这里不需要手写
    }

    private BlogSetting empty() {
        BlogSetting s = new BlogSetting();
        s.setId(BlogSettingConsts.SINGLETON_ID);
        s.setSiteName("");
        s.setSiteSubtitle("");
        s.setLogoUrl("");
        s.setLogoRadius(BlogSettingConsts.DEFAULT_LOGO_RADIUS);
        s.setThemeColor(BlogSettingConsts.DEFAULT_THEME_COLOR);
        s.setCopyright("");
        s.setIcp("");
        s.setIcpUrl("");
        s.setPoliceIcp("");
        s.setPoliceIcpUrl("");
        s.setSeoKeywords("");
        s.setSeoDescription("");
        s.setCommentEnabled(BlogSettingConsts.COMMENT_ENABLED);
        s.setOwnerNickname(BlogSettingConsts.DEFAULT_OWNER_NICKNAME);
        // 设置行缺失时给出默认名单：与建表默认值一致，避免首次升级后「什么都没配」而静默放行
        s.setReservedNicknames(BlogSettingConsts.DEFAULT_RESERVED_NICKNAMES_TEXT);
        return s;
    }

    /** 链接补全协议：填 beian.miit.gov.cn 也能跳转 */
    private String normalizeLink(String url) {
        String value = trim(url);
        if (value.isEmpty()) {
            return "";
        }
        String lower = value.toLowerCase();
        if (lower.startsWith(HttpConsts.PREFIX_HTTP) || lower.startsWith(HttpConsts.PREFIX_HTTPS)) {
            return value;
        }
        return HttpConsts.PREFIX_HTTPS + value;
    }

    /** 圆角兜底与裁剪：非法回默认，0 为合法值（直角） */
    private int normalizeRadius(Integer radius) {
        if (radius == null || radius < 0) {
            return BlogSettingConsts.DEFAULT_LOGO_RADIUS;
        }
        return Math.min(radius, BlogSettingConsts.MAX_LOGO_RADIUS);
    }

    /** 开关归一化：只认 0 为关闭，其余（含 null）按开启处理 */
    private int normalizeEnabled(Integer enabled) {
        return (enabled != null && enabled == 0) ? 0 : BlogSettingConsts.COMMENT_ENABLED;
    }

    /** 主题色：仅接受 #RRGGBB，非法 / 留空回默认 */
    private String normalizeColor(String color) {
        String value = trim(color);
        return HEX_COLOR.matcher(value).matches() ? value.toUpperCase() : BlogSettingConsts.DEFAULT_THEME_COLOR;
    }

    /**
     * 署名归一化：清洗 → 空值回默认 → 超长截断。
     *
     * <p>空值必须回落到「博主」而非留空：一个空署名会让后台回复变成无名评论，
     * 也会让保留昵称校验失去目标。</p>
     */
    private String ownerNickname(String raw) {
        String name = NicknameUtil.sanitize(raw);
        if (name.isEmpty()) {
            return BlogSettingConsts.DEFAULT_OWNER_NICKNAME;
        }
        return name.length() > BlogSettingConsts.MAX_OWNER_NICKNAME_LENGTH
                ? name.substring(0, BlogSettingConsts.MAX_OWNER_NICKNAME_LENGTH)
                : name;
    }

    /**
     * 保留昵称配置归一化：拆分 → 清洗去重 → 按逗号回写。
     *
     * <p>回写归一化结果是刻意的：站长用全角逗号、「；」或换行分隔时，存进去的是统一格式，
     * 下次读出来既能直接展示、也能被 {@code NicknameUtil.parse} 稳定解析。
     * 全空（清空输入框）会存成空串 —— 表示不拦截任何词，这是站长的显式选择。</p>
     */
    private String normalizeReserved(String raw) {
        String joined = String.join(",", NicknameUtil.parse(raw));
        if (joined.length() > BlogSettingConsts.MAX_RESERVED_LENGTH) {
            throw new BizException("保留昵称配置过长，最多 " + BlogSettingConsts.MAX_RESERVED_LENGTH + " 个字符");
        }
        return joined;
    }

    private String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
