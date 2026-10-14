package com.designer.admin.module.blog.mapper;

import com.designer.admin.module.blog.model.vo.VisitNameValueVO;
import com.designer.admin.module.blog.model.vo.VisitRefererVO;
import com.designer.admin.module.blog.model.vo.VisitTrendVO;
import com.github.yulichang.base.MPJBaseMapper;
import com.designer.admin.module.blog.model.entity.BlogVisitLog;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 访问日志 Mapper。
 *
 * <p>统计查询都是聚合，用 MyBatis-Plus 的 Wrapper 表达不了，故直接写注解 SQL。
 * 所有查询都以 {@code visit_date >= #{from}} 为前缀条件，命中 {@code idx_blog_visit_date}。</p>
 */
public interface BlogVisitMapper extends MPJBaseMapper<BlogVisitLog> {

    /** 区间浏览量（PV） */
    @Select("SELECT COUNT(1) FROM blog_visit_log WHERE visit_date >= #{from}")
    long countPv(@Param("from") LocalDate from);

    /** 区间独立访客数（UV）：按访客标识去重 */
    @Select("SELECT COUNT(DISTINCT visitor_key) FROM blog_visit_log WHERE visit_date >= #{from}")
    long countUv(@Param("from") LocalDate from);

    /** 指定单日的 PV */
    @Select("SELECT COUNT(1) FROM blog_visit_log WHERE visit_date = #{date}")
    long countPvOn(@Param("date") LocalDate date);

    /** 指定单日的 UV */
    @Select("SELECT COUNT(DISTINCT visitor_key) FROM blog_visit_log WHERE visit_date = #{date}")
    long countUvOn(@Param("date") LocalDate date);

    /**
     * PV / UV 按天趋势。
     *
     * <p>用 {@code DATE_FORMAT} 直接出字符串：日期交给 SQL 格式化，
     * 既避免 LocalDate 被序列化成数组，也让前端不必再处理时区。</p>
     */
    @Select("""
            SELECT DATE_FORMAT(visit_date, '%Y-%m-%d') AS date,
                   COUNT(1)                        AS pv,
                   COUNT(DISTINCT visitor_key)      AS uv
            FROM blog_visit_log
            WHERE visit_date >= #{from}
            GROUP BY visit_date
            ORDER BY visit_date
            """)
    List<VisitTrendVO> selectTrend(@Param("from") LocalDate from);

    /**
     * 分类访问分布。
     *
     * <p>只有文章详情页会写入 {@code category_id}，所以这个图反映的是
     * 「各分类下文章被访问的量」，首页/列表页的流量不计入。</p>
     */
    @Select("""
            SELECT COALESCE(c.name, '未知分类') AS name,
                   COUNT(1)                     AS `value`
            FROM blog_visit_log v
            LEFT JOIN blog_category c ON c.id = v.category_id
            WHERE v.visit_date >= #{from} AND v.category_id IS NOT NULL
            GROUP BY v.category_id, c.name
            ORDER BY `value` DESC
            LIMIT #{size}
            """)
    List<VisitNameValueVO> selectCategoryRank(@Param("from") LocalDate from, @Param("size") int size);

    /** 来源域名 Top N（含直接访问：host 为空串） */
    @Select("""
            SELECT referer_host AS host,
                   referer_type AS type,
                   COUNT(1)     AS `value`
            FROM blog_visit_log
            WHERE visit_date >= #{from}
            GROUP BY referer_host, referer_type
            ORDER BY `value` DESC
            LIMIT #{size}
            """)
    List<VisitRefererVO> selectRefererRank(@Param("from") LocalDate from, @Param("size") int size);

    /** 设备占比 */
    @Select("""
            SELECT device AS name,
                   COUNT(1) AS `value`
            FROM blog_visit_log
            WHERE visit_date >= #{from}
            GROUP BY device
            ORDER BY `value` DESC
            """)
    List<VisitNameValueVO> selectDeviceRank(@Param("from") LocalDate from);

    /** 地域分布 Top N（归属地解析不到的记为「未知」，不参与排行） */
    @Select("""
            SELECT ip_location AS name,
                   COUNT(1)     AS `value`
            FROM blog_visit_log
            WHERE visit_date >= #{from} AND ip_location <> ''
            GROUP BY ip_location
            ORDER BY `value` DESC
            LIMIT #{size}
            """)
    List<VisitNameValueVO> selectRegionRank(@Param("from") LocalDate from, @Param("size") int size);

    /** 清理保留期之外的日志，返回删除行数 */
    @Delete("DELETE FROM blog_visit_log WHERE visit_date < #{before}")
    int deleteBefore(@Param("before") LocalDate before);
}
