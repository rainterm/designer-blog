package com.designer.admin.module.blog.service;

import com.designer.admin.common.exception.BizException;
import com.designer.admin.module.blog.constant.BlogCacheTags;
import com.designer.admin.module.blog.mapper.BlogSettingMapper;
import com.designer.admin.module.blog.model.dto.SettingSaveDTO;
import com.designer.admin.module.blog.model.entity.BlogSetting;
import com.designer.admin.module.blog.model.vo.PublicSiteVO;
import com.designer.admin.module.blog.util.NicknameUtil;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.data.annotation.CacheRemove;

import java.util.List;

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

    /** 站长评论署名默认值：同时会被自动加入保留昵称名单 */
    private static final String DEFAULT_OWNER_NICKNAME = "博主";

    /** 署名长度上限，与 blog_setting.owner_nickname / blog_comment.nickname 列宽一致 */
    private static final int MAX_OWNER_NICKNAME_LENGTH = 50;

    /**
     * 保留昵称配置的长度上限，与 {@code blog_setting.reserved_nicknames} 列宽一致。
     *
     * <p>超限直接报错而不是静默截断：截断会悄悄丢掉排在后面的词，
     * 站长以为配了、实际没生效，比报错更难排查。</p>
     */
    private static final int MAX_RESERVED_LENGTH = 500;

    @Db
    private BlogSettingMapper settingMapper;

    /**
     * 站点设置读取。
     *
     * <p>这里的读缓存由 {@code BlogPublicController#site} 上的 {@code @Cache} 承担：
     * 那个接口是这条数据唯一的读放大来源（SSR 每次渲染一次）。本方法自身不再缓存，
     * 所以 {@code getPublic()} / {@code isCommentEnabled()} 的内部调用是直接查库的
     * —— 前者被 controller 缓存挡住，后者只在提交评论时走一次。</p>
     */
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
        String owner = ownerNickname(s.getOwnerNickname());
        vo.setOwnerNickname(owner);
        // 名单与拦截同源：BlogCommentService 提交校验时也走 NicknameUtil.reserved，
        // 所以前台拿到的名单不会与后端实际规则脱节
        vo.setReservedNicknames(NicknameUtil.reserved(s.getReservedNicknames(), owner));
        return vo;
    }

    /**
     * 站长评论署名（默认「博主」）。
     *
     * <p>评论模块用它做两件事：后台回复的默认署名，以及（自动加入的）保留昵称。
     * 统一走这里，避免两处各取一次配置得出不同结果。</p>
     */
    public String getOwnerNickname() {
        return ownerNickname(get().getOwnerNickname());
    }

    /**
     * 生效的保留昵称名单：后台配置 + 站长署名（自动追加并去重）。
     *
     * <p>评论模块提交校验与 {@link #getPublic()} 下发给前台的都取自这里，
     * 一次 {@link #get()} 读出全部原始字段，避免多次查库期间配置被改而拼出不一致的名单。</p>
     */
    public List<String> getReservedNicknames() {
        BlogSetting s = get();
        return NicknameUtil.reserved(s.getReservedNicknames(), ownerNickname(s.getOwnerNickname()));
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
    @CacheRemove(tags = BlogCacheTags.SITE)
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
        entity.setOwnerNickname(ownerNickname(dto.getOwnerNickname()));
        entity.setReservedNicknames(normalizeReserved(dto.getReservedNicknames()));

        if (settingMapper.selectById(SINGLETON_ID) == null) {
            settingMapper.insert(entity);
        } else {
            settingMapper.updateById(entity);
        }
        // 失效由方法上的 @CacheRemove 在方法返回后执行，这里不需要手写
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
        s.setOwnerNickname(DEFAULT_OWNER_NICKNAME);
        // 设置行缺失时给出默认名单：与建表默认值一致，避免首次升级后「什么都没配」而静默放行
        s.setReservedNicknames(NicknameUtil.DEFAULT_RESERVED_TEXT);
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

    /**
     * 署名归一化：清洗 → 空值回默认 → 超长截断。
     *
     * <p>空值必须回落到「博主」而非留空：一个空署名会让后台回复变成无名评论，
     * 也会让保留昵称校验失去目标。</p>
     */
    private String ownerNickname(String raw) {
        String name = NicknameUtil.sanitize(raw);
        if (name.isEmpty()) {
            return DEFAULT_OWNER_NICKNAME;
        }
        return name.length() > MAX_OWNER_NICKNAME_LENGTH
                ? name.substring(0, MAX_OWNER_NICKNAME_LENGTH)
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
        if (joined.length() > MAX_RESERVED_LENGTH) {
            throw new BizException("保留昵称配置过长，最多 " + MAX_RESERVED_LENGTH + " 个字符");
        }
        return joined;
    }

    private String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
