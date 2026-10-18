package com.designer.admin.module.blog.service;

import com.designer.admin.module.blog.model.dto.SettingSaveDTO;
import com.designer.admin.module.blog.model.entity.BlogSetting;
import com.designer.admin.module.blog.model.vo.PublicSiteVO;

import java.util.List;

/**
 * 博客站点设置（单行）：站点名称 / Logo / 主题色 / 页脚版权 / 备案号
 *
 * @author 雨夜终端
 */
public interface BlogSettingService {

    /**
     * 读取站点设置（单行配置），缺省时返回默认值对象。
     *
     * @return 站点设置
     */
    BlogSetting getSetting();

    /**
     * 前台公开站点设置。
     *
     * @return 公开站点信息
     */
    PublicSiteVO getPublic();

    /**
     * 站长评论署名。
     *
     * @return 署名；未配置时返回默认值
     */
    String getOwnerNickname();

    /**
     * 保留昵称列表（保留词配置 + 站长署名，已去重）。
     *
     * @return 保留昵称列表
     */
    List<String> listReservedNicknames();

    /**
     * 评论总开关是否开启。
     *
     * @return 开启返回 true
     */
    boolean isCommentEnabled();

    /**
     * 保存站点设置并失效前台站点信息缓存。
     *
     * @param dto 保存参数
     */
    void update(SettingSaveDTO dto);
}
