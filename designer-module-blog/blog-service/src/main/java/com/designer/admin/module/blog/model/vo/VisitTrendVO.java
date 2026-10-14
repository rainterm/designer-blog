package com.designer.admin.module.blog.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 按天的访问趋势点（同时用作 Mapper 的查询结果载体）。
 */
@Data
public class VisitTrendVO implements Serializable {

    /** 日期，格式 yyyy-MM-dd（SQL 里已格式化，避免序列化成数组） */
    private String date;

    /** 当日浏览量 */
    private long pv;

    /** 当日独立访客数 */
    private long uv;
}
