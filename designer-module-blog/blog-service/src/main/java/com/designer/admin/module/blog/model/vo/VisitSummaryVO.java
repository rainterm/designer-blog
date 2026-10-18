package com.designer.admin.module.blog.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 访问量概览数字（统计页顶部卡片）
 *
 * @author 雨夜终端
 */
@Data
public class VisitSummaryVO implements Serializable {

    /** 区间内总浏览量 */
    private Long pv;

    /** 区间内独立访客数 */
    private Long uv;

    /** 今日浏览量 */
    private Long todayPv;

    /** 今日独立访客数 */
    private Long todayUv;
}
