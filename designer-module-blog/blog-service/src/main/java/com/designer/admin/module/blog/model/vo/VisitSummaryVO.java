package com.designer.admin.module.blog.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 访问量概览数字（统计页顶部卡片）
 */
@Data
public class VisitSummaryVO implements Serializable {

    /** 区间内总浏览量 */
    private long pv;

    /** 区间内独立访客数 */
    private long uv;

    /** 今日浏览量 */
    private long todayPv;

    /** 今日独立访客数 */
    private long todayUv;
}
