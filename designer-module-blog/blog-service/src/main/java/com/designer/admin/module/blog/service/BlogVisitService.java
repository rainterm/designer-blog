package com.designer.admin.module.blog.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.module.blog.model.dto.VisitLogQueryDTO;
import com.designer.admin.module.blog.model.dto.VisitReportDTO;
import com.designer.admin.module.blog.model.vo.VisitLogVO;
import com.designer.admin.module.blog.model.vo.VisitOverviewVO;

/**
 * 博客访问统计。
 *
 * <p>写入侧（{@link #addVisit}）由前台匿名接口触发，**永不抛异常**：
 * 埋点是附加能力，任何失败都不该让访客的页面报错。
 * 读取侧（{@link #getOverview}）供后台统计页使用，一次返回全部图表数据。</p>
 *
 * @author 雨夜终端
 */
public interface BlogVisitService {

    /**
     * 记录一次访问。
     *
     * <p>除路径外全部由服务端推导：IP / UA 取自请求头，设备与来源由它们解析，
     * 文章与分类按 slug 反查。前端只能说明「看了哪个路径」。
     * 埋点失败只记日志，不影响访客的正常请求。</p>
     *
     * @param dto 上报参数（页面路径）
     */
    void addVisit(VisitReportDTO dto);

    /**
     * 统计概览：一次返回统计页需要的全部图表数据。
     *
     * @param days 统计最近多少天（含今天），超出范围会被归一化
     * @return 概览数据
     */
    VisitOverviewVO getOverview(Integer days);

    /**
     * 分页查询访问明细。
     *
     * <p>与概览页的分工：概览只做聚合，本方法保留每条流水，
     * 用于排查「某个 IP 为什么访问量异常」「这条来源到底来自哪」这类具体问题。</p>
     *
     * @param query 筛选与分页条件
     * @return 访问明细分页
     */
    IPage<VisitLogVO> listLogs(VisitLogQueryDTO query);

    /**
     * 清理保留期之外的访问日志。
     *
     * @param days 保留天数；为空或小于 1 时使用默认值
     * @return 删除行数
     */
    int removeOlderThan(Integer days);
}
