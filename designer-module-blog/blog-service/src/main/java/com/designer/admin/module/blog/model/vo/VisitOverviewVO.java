package com.designer.admin.module.blog.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 访问统计概览：一次请求返回统计页需要的全部图表数据。
 *
 * <p>统计页是多图并存，拆成多个接口会让「同一时间窗口」难以保证一致，
 * 也会放大往返次数，故合并为一个响应体。</p>
 */
@Data
public class VisitOverviewVO implements Serializable {

    /** 区间天数（后端归一化后的实际值） */
    private int days;

    private VisitSummaryVO summary;

    /** PV / UV 按天趋势 */
    private List<VisitTrendVO> trend;

    /** 分类访问分布（仅文章详情页有分类，其余页面不计入） */
    private List<VisitNameValueVO> categories;

    /** 来源域名 Top（host 为空串代表直接访问） */
    private List<VisitRefererVO> referers;

    /** 设备占比：PC / MOBILE / TABLET / OTHER */
    private List<VisitNameValueVO> devices;

    /** 访客地域分布（Top N 省份） */
    private List<VisitNameValueVO> regions;
}
