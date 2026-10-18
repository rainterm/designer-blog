package com.designer.admin.module.blog.mapper;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.designer.admin.module.blog.model.entity.BlogCategory;
import com.designer.admin.module.blog.model.entity.BlogVisitLog;
import com.designer.admin.module.blog.model.vo.VisitNameValueVO;
import com.designer.admin.module.blog.model.vo.VisitRefererVO;
import com.designer.admin.module.blog.model.vo.VisitTrendVO;
import com.github.yulichang.base.MPJBaseMapper;
import com.github.yulichang.toolkit.JoinWrappers;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 访问日志 Mapper。
 *
 * @author 雨夜终端
 */
public interface BlogVisitMapper extends MPJBaseMapper<BlogVisitLog> {

    /**
     * 统计区间浏览量（PV）。
     *
     * @param from 起始日期（含）
     * @return 浏览量
     */
    default long countPv(LocalDate from) {
        return selectCount(Wrappers.<BlogVisitLog>lambdaQuery()
                .ge(BlogVisitLog::getVisitDate, from));
    }

    /**
     * 统计区间独立访客数（UV），按访客标识去重。
     *
     * @param from 起始日期（含）
     * @return 独立访客数
     */
    default long countUv(LocalDate from) {
        Long uv = selectJoinOne(Long.class, JoinWrappers.lambda(BlogVisitLog.class)
                .selectFunc("COUNT(DISTINCT %s)", w -> w.accept(BlogVisitLog::getVisitorKey), "uv")
                .ge(BlogVisitLog::getVisitDate, from));
        return uv == null ? 0L : uv;
    }

    /**
     * 统计指定单日的 PV。
     *
     * @param date 统计日期
     * @return 浏览量
     */
    default long countPvOn(LocalDate date) {
        return selectCount(Wrappers.<BlogVisitLog>lambdaQuery()
                .eq(BlogVisitLog::getVisitDate, date));
    }

    /**
     * 统计指定单日的 UV。
     *
     * @param date 统计日期
     * @return 独立访客数
     */
    default long countUvOn(LocalDate date) {
        Long uv = selectJoinOne(Long.class, JoinWrappers.lambda(BlogVisitLog.class)
                .selectFunc("COUNT(DISTINCT %s)", w -> w.accept(BlogVisitLog::getVisitorKey), "uv")
                .eq(BlogVisitLog::getVisitDate, date));
        return uv == null ? 0L : uv;
    }

    /**
     * PV / UV 按天趋势。
     *
     * <p>日期直接取 {@link LocalDate}，由 {@link VisitTrendVO} 上的
     * {@code @ONodeAttr} 负责格式化成 {@code yyyy-MM-dd}，SQL 里不出现日期函数。</p>
     *
     * @param from 起始日期（含）
     * @return 按日期升序排列的趋势数据
     */
    default List<VisitTrendVO> selectTrend(LocalDate from) {
        return selectJoinList(VisitTrendVO.class, JoinWrappers.lambda(BlogVisitLog.class)
                .selectAs(BlogVisitLog::getVisitDate, VisitTrendVO::getDate)
                .selectCount(BlogVisitLog::getId, "pv")
                .selectFunc("COUNT(DISTINCT %s)", w -> w.accept(BlogVisitLog::getVisitorKey), "uv")
                .ge(BlogVisitLog::getVisitDate, from)
                .groupBy(BlogVisitLog::getVisitDate)
                .orderByAsc(BlogVisitLog::getVisitDate));
    }

    /**
     * 分类访问分布。
     *
     * <p>只有文章详情页会写入 {@code category_id}，所以这个图反映的是
     * 「各分类下文章被访问的量」，首页/列表页的流量不计入。</p>
     *
     * @param from 起始日期（含）
     * @param size 返回条数上限
     * @return 按访问量降序排列的分类分布
     */
    default List<VisitNameValueVO> selectCategoryRank(LocalDate from, int size) {
        return selectJoinPage(topPage(size, "value"), VisitNameValueVO.class,
                JoinWrappers.lambda(BlogVisitLog.class)
                        .leftJoin(BlogCategory.class, BlogCategory::getId, BlogVisitLog::getCategoryId)
                        .selectFunc("COALESCE(%s, '未知分类')", w -> w.accept(BlogCategory::getName), "name")
                        .selectCount(BlogVisitLog::getId, "value")
                        .ge(BlogVisitLog::getVisitDate, from)
                        .isNotNull(BlogVisitLog::getCategoryId)
                        .groupBy(BlogVisitLog::getCategoryId)
                        .groupBy(BlogCategory::getName))
                .getRecords();
    }

    /**
     * 来源域名 Top N（含直接访问：host 为空串）。
     *
     * @param from 起始日期（含）
     * @param size 返回条数上限
     * @return 按访问量降序排列的来源分布
     */
    default List<VisitRefererVO> selectRefererRank(LocalDate from, int size) {
        return selectJoinPage(topPage(size, "value"), VisitRefererVO.class,
                JoinWrappers.lambda(BlogVisitLog.class)
                        .selectAs(BlogVisitLog::getRefererHost, VisitRefererVO::getHost)
                        .selectAs(BlogVisitLog::getRefererType, VisitRefererVO::getType)
                        .selectCount(BlogVisitLog::getId, "value")
                        .ge(BlogVisitLog::getVisitDate, from)
                        .groupBy(BlogVisitLog::getRefererHost, BlogVisitLog::getRefererType))
                .getRecords();
    }

    /**
     * 统计设备占比。
     *
     * @param from 起始日期（含）
     * @return 按访问量降序排列的设备分布
     */
    default List<VisitNameValueVO> selectDeviceRank(LocalDate from) {
        return selectJoinList(VisitNameValueVO.class, JoinWrappers.lambda(BlogVisitLog.class)
                .selectAs(BlogVisitLog::getDevice, VisitNameValueVO::getName)
                .selectCount(BlogVisitLog::getId, "value")
                .ge(BlogVisitLog::getVisitDate, from)
                .groupBy(BlogVisitLog::getDevice)
                .last("ORDER BY value DESC"));
    }

    /**
     * 地域分布 Top N（归属地解析不到的记为「未知」，不参与排行）。
     *
     * @param from 起始日期（含）
     * @param size 返回条数上限
     * @return 按访问量降序排列的地域分布
     */
    default List<VisitNameValueVO> selectRegionRank(LocalDate from, int size) {
        return selectJoinPage(topPage(size, "value"), VisitNameValueVO.class,
                JoinWrappers.lambda(BlogVisitLog.class)
                        .selectAs(BlogVisitLog::getIpLocation, VisitNameValueVO::getName)
                        .selectCount(BlogVisitLog::getId, "value")
                        .ge(BlogVisitLog::getVisitDate, from)
                        .ne(BlogVisitLog::getIpLocation, "")
                        .groupBy(BlogVisitLog::getIpLocation))
                .getRecords();
    }

    /**
     * 构造「按聚合别名降序、取前 {@code size} 条」的分页参数。
     *
     * <p>{@code searchCount=false}：Top N 只关心前几条，不需要总数，
     * 否则分页插件会额外发一条 count SQL。行数限制由分页插件按方言生成。</p>
     */
    private static <T> Page<T> topPage(int size, String orderColumn) {
        Page<T> page = new Page<>(1, size, false);
        page.addOrder(OrderItem.desc(orderColumn));
        return page;
    }

    /**
     * 清理保留期之外的日志。
     *
     * <p>刻意写成物理删除：访问日志是明细流水，保留期清理的目的就是释放存储。
     * 实体继承的 {@code @TableLogic} 会把 Wrapper 删除改写成 {@code UPDATE deleted = 1}，
     * 行只增不减，清理就失去意义，故这里用注解 SQL 直接 {@code DELETE}。</p>
     *
     * @param before 保留边界，早于该日期的记录会被删除
     * @return 删除行数
     */
    @Delete("DELETE FROM blog_visit_log WHERE visit_date < #{before}")
    int deleteBefore(@Param("before") LocalDate before);
}
