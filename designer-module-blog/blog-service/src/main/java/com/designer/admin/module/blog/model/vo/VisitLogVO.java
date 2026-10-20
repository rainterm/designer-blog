package com.designer.admin.module.blog.model.vo;

import com.designer.admin.common.core.DateTimePattern;
import lombok.Data;
import org.noear.snack4.annotation.ONodeAttr;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 后台访问记录（明细）展示对象。
 *
 * <p>不直接返回 {@code BlogVisitLog} 实体：
 * <ul>
 *   <li>{@code visitorKey} 是 IP + UA 的哈希，只用于 UV 去重，后台没有展示价值；</li>
 *   <li>库里存的是与语言无关的码（{@code post} / {@code MOBILE} / {@code direct}），
 *       展示文案由服务端按数据字典统一转换，前端不再各自维护一份映射。</li>
 * </ul>
 *
 * @author 雨夜终端
 */
@Data
public class VisitLogVO implements Serializable {

    /** 记录ID */
    private Long id;

    /** 访问日期，序列化为 yyyy-MM-dd */
    @ONodeAttr(format = DateTimePattern.DATE)
    private LocalDate visitDate;

    /** 页面类型码：home / post / tag / category / other */
    private String pageType;

    /** 页面类型文案，取自字典 blog_visit_page_type */
    private String pageTypeLabel;

    /** 访问路径（已去 query 与锚点） */
    private String path;

    /** 文章ID（仅详情页有值） */
    private Long postId;

    /** 客户端 IP */
    private String ip;

    /** IP 归属地（省级） */
    private String ipLocation;

    /** 设备码：PC / MOBILE / TABLET / BOT / OTHER */
    private String device;

    /** 设备文案，取自字典 blog_visit_device */
    private String deviceLabel;

    /** 来源域名，直接访问或站内跳转为空串 */
    private String refererHost;

    /** 来源类型码：direct / search / external */
    private String refererType;

    /** 来源类型文案，取自字典 blog_visit_referer_type */
    private String refererTypeLabel;

    /** User-Agent（截断 255） */
    private String ua;

    /** 记录时间，序列化为 yyyy-MM-dd HH:mm:ss */
    @ONodeAttr(format = DateTimePattern.DATE_TIME)
    private LocalDateTime createTime;
}
