package com.designer.admin.module.blog.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.designer.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 博客访问日志（每次来访一条明细）。
 *
 * <p>明细表会持续增长，配套 {@code blogLogTask.cleanVisitLog(days)} 做保留期清理。</p>
 *
 * <p>除 {@code path} 由前端上报外，其余字段一律**服务端从请求推导**：
 * IP / UA 取自请求头，设备与来源由它们解析，文章与分类按 slug 反查数据库。
 * 前端只能说明「访问了哪个路径」，不能伪造自己的 IP、来源或归属分类。</p>
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("blog_visit_log")
public class BlogVisitLog extends BaseEntity {

    /** 访问日期：冗余列，按天聚合直接走索引，不必从 create_time 现算 */
    private LocalDate visitDate;

    /** 页面类型：home / post / tag / category / other */
    private String pageType;

    /** 访问路径（已去 query 与锚点） */
    private String path;

    /** 文章ID（仅详情页，按 slug 反查） */
    private Long postId;

    /** 文章分类ID（仅详情页，用于分类访问分布） */
    private Long categoryId;

    /** 客户端 IP */
    private String ip;

    /** IP 归属地（省级） */
    private String ipLocation;

    /** 访客标识：IP + UA 的 SHA-256，用于 UV 去重 */
    private String visitorKey;

    /** 设备：PC / MOBILE / TABLET / OTHER */
    private String device;

    /** 来源域名，直接访问或站内跳转为空串 */
    private String refererHost;

    /** 来源类型：direct / search / external */
    private String refererType;

    /** User-Agent（截断 255，便于排查） */
    private String ua;
}
