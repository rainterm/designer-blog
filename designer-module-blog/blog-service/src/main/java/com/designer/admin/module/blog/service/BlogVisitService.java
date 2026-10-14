package com.designer.admin.module.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.designer.admin.framework.client.IpRegionResolver;
import com.designer.admin.module.blog.mapper.BlogPostMapper;
import com.designer.admin.module.blog.mapper.BlogVisitMapper;
import com.designer.admin.module.blog.model.dto.VisitReportDTO;
import com.designer.admin.module.blog.model.entity.BlogPost;
import com.designer.admin.module.blog.model.entity.BlogVisitLog;
import com.designer.admin.module.blog.model.vo.VisitNameValueVO;
import com.designer.admin.module.blog.model.vo.VisitOverviewVO;
import com.designer.admin.module.blog.model.vo.VisitSummaryVO;
import com.designer.admin.module.blog.model.vo.VisitTrendVO;
import com.designer.admin.module.blog.util.ClientIpUtil;
import com.designer.admin.module.blog.util.HashUtil;
import com.designer.admin.module.blog.util.RefererUtil;
import com.designer.admin.module.blog.util.UserAgentUtil;
import com.designer.admin.module.blog.util.VisitPathUtil;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.redisx.RedisClient;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.core.handle.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 博客访问统计。
 *
 * <p>写入侧（{@link #record}）由前台匿名接口触发，**永不抛异常**：
 * 埋点是附加能力，任何失败都不该让访客的页面报错。
 * 读取侧（{@link #overview}）供后台统计页使用，一次返回全部图表数据。</p>
 */
@Component
public class BlogVisitService {

    private static final Logger log = LoggerFactory.getLogger(BlogVisitService.class);

    /** 去重键：同一访客对同一路径在该时间窗内只记一次 */
    private static final String DEDUP_KEY = "blog:visit:dedup:";
    /** 去重窗口（秒）：覆盖刷新、重复上报与前端路由抖动 */
    private static final int DEDUP_TTL = 60;
    /** 单 IP 每日配额键 */
    private static final String QUOTA_KEY = "blog:visit:quota:";
    /** 单 IP 每日记录上限：正常阅读远达不到，只为挡住脚本刷量 */
    private static final int DAILY_QUOTA = 3000;
    private static final int DAY_SECONDS = 24 * 60 * 60;

    /** 统计区间默认天数 */
    private static final int DEFAULT_DAYS = 30;
    private static final int MAX_DAYS = 365;
    /** 排行榜默认取前 N 名 */
    private static final int TOP_SIZE = 10;
    /** 日志保留天数默认值（清理任务未传参时使用） */
    private static final int DEFAULT_KEEP_DAYS = 90;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Db
    private BlogVisitMapper visitMapper;
    @Db
    private BlogPostMapper postMapper;
    @Inject
    private IpRegionResolver ipRegionResolver;
    @Inject
    private RedisClient redisClient;

    /**
     * 记录一次访问。
     *
     * <p>除路径外全部由服务端推导：IP / UA 取自请求头，设备与来源由它们解析，
     * 文章与分类按 slug 反查。前端只能说明「看了哪个路径」。</p>
     */
    public void record(VisitReportDTO dto) {
        try {
            doRecord(dto);
        } catch (Exception e) {
            // 埋点失败不能影响访客的正常请求，只留日志
            log.warn("[blog-visit] 埋点写入失败：{}", e.getMessage());
        }
    }

    private void doRecord(VisitReportDTO dto) {
        Context ctx = Context.current();
        if (ctx == null) {
            return;
        }
        String rawPath = dto == null ? null : dto.getPath();
        // 前端必带 path；缺失就跳过，「宁可不记，也不要把接口路径当成页面」
        VisitPathUtil.Parsed parsed = VisitPathUtil.parse(rawPath);
        if (parsed.path().isEmpty()) {
            return;
        }

        Long postId = null;
        Long categoryId = null;
        if (parsed.type() == VisitPathUtil.PageType.POST) {
            BlogPost post = postMapper.selectOne(new LambdaQueryWrapper<BlogPost>()
                    .select(BlogPost::getId, BlogPost::getCategoryId)
                    .eq(BlogPost::getSlug, parsed.slug())
                    .eq(BlogPost::getStatus, 1)
                    .last("limit 1"));
            if (post == null) {
                // 文章不存在或未发布：归入 other，避免无效 URL 把文章/分类统计刷高
                parsed = new VisitPathUtil.Parsed(VisitPathUtil.PageType.OTHER, parsed.path(), "");
            } else {
                postId = post.getId();
                categoryId = post.getCategoryId();
            }
        }

        String ip = ClientIpUtil.normalize(ctx);
        String ua = UserAgentUtil.truncate(ctx.header("User-Agent"));
        String visitorKey = HashUtil.sha256Hex(ip + "|" + ua);

        // 去重先于配额：刷新 / 重复上报在这一步就被挡掉，不消耗配额
        if (incrWithTtl(DEDUP_KEY + visitorKey + ":" + parsed.path(), DEDUP_TTL) > 1) {
            return;
        }
        if (incrWithTtl(QUOTA_KEY + ip, DAY_SECONDS) > DAILY_QUOTA) {
            // 超配额静默丢弃：对访客毫无感知，也不会污染统计
            return;
        }

        String refererHost = RefererUtil.host(ctx.header("Referer"), selfHost(ctx));

        BlogVisitLog entity = new BlogVisitLog();
        entity.setVisitDate(LocalDate.now());
        entity.setPageType(parsed.type().getCode());
        entity.setPath(parsed.path());
        entity.setPostId(postId);
        entity.setCategoryId(categoryId);
        entity.setIp(ip);
        // 归属地在写入时解析并固化，与评论保持一致：日后换数据文件不影响历史
        entity.setIpLocation(ipRegionResolver.lookup(ip));
        entity.setVisitorKey(visitorKey);
        entity.setDevice(UserAgentUtil.device(ua));
        entity.setRefererHost(refererHost);
        entity.setRefererType(RefererUtil.type(refererHost));
        entity.setUa(ua);
        visitMapper.insert(entity);
    }

    /**
     * 统计概览：一次返回统计页需要的全部图表数据。
     *
     * @param days 统计最近多少天（含今天），超范围会被归一化
     */
    public VisitOverviewVO overview(Integer days) {
        int span = normalizeDays(days);
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(span - 1L);

        VisitOverviewVO vo = new VisitOverviewVO();
        vo.setDays(span);
        vo.setSummary(buildSummary(from, today));
        // 趋势补齐空缺日期：图表缺日会断线，视觉上像丢数据
        vo.setTrend(fillMissingDays(visitMapper.selectTrend(from), from, today));
        vo.setCategories(visitMapper.selectCategoryRank(from, TOP_SIZE));
        vo.setReferers(visitMapper.selectRefererRank(from, TOP_SIZE));
        vo.setDevices(withDeviceLabels(visitMapper.selectDeviceRank(from)));
        vo.setRegions(visitMapper.selectRegionRank(from, TOP_SIZE));
        return vo;
    }

    /** 清理保留期之外的访问日志，返回删除行数 */
    public int cleanOlderThan(Integer days) {
        int keepDays = (days == null || days < 1) ? DEFAULT_KEEP_DAYS : days;
        return visitMapper.deleteBefore(LocalDate.now().minusDays(keepDays));
    }

    // ------------------------------------------------------------------
    // 内部方法
    // ------------------------------------------------------------------

    private VisitSummaryVO buildSummary(LocalDate from, LocalDate today) {
        VisitSummaryVO summary = new VisitSummaryVO();
        summary.setPv(visitMapper.countPv(from));
        summary.setUv(visitMapper.countUv(from));
        summary.setTodayPv(visitMapper.countPvOn(today));
        summary.setTodayUv(visitMapper.countUvOn(today));
        return summary;
    }

    /**
     * 当前站点自身域名：用于把站内跳转从「来源」里剔除。
     *
     * <p>优先取 {@code Host} 请求头（HTTP/1.1 必有），拿不到再退回请求 URI。</p>
     */
    private String selfHost(Context ctx) {
        String host = ctx.header("Host");
        if (host != null && !host.isBlank()) {
            return host;
        }
        URI uri = ctx.uri();
        return uri == null || uri.getHost() == null ? "" : uri.getHost();
    }

    /** 把查询结果按日期铺平成连续区间，缺失的日期补 0 */
    private List<VisitTrendVO> fillMissingDays(List<VisitTrendVO> rows, LocalDate from, LocalDate to) {
        Map<String, VisitTrendVO> byDate = new LinkedHashMap<>();
        for (VisitTrendVO row : rows) {
            byDate.put(row.getDate(), row);
        }
        List<VisitTrendVO> filled = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            String key = d.format(DATE_FMT);
            VisitTrendVO row = byDate.get(key);
            if (row == null) {
                row = new VisitTrendVO();
                row.setDate(key);
            }
            filled.add(row);
        }
        return filled;
    }

    /**
     * 设备码转中文标签。
     *
     * <p>库里存的是 {@code PC / MOBILE / TABLET / BOT / OTHER} 这类语言无关的码，
     * 展示文案在这里定，避免把中文写进数据。</p>
     */
    private List<VisitNameValueVO> withDeviceLabels(List<VisitNameValueVO> rows) {
        for (VisitNameValueVO row : rows) {
            row.setName(switch (row.getName() == null ? "" : row.getName()) {
                case UserAgentUtil.PC -> "电脑";
                case UserAgentUtil.MOBILE -> "手机";
                case UserAgentUtil.TABLET -> "平板";
                case UserAgentUtil.BOT -> "机器人";
                default -> "其他";
            });
        }
        return rows;
    }

    private int normalizeDays(Integer days) {
        if (days == null || days < 1) {
            return DEFAULT_DAYS;
        }
        return Math.min(days, MAX_DAYS);
    }

    /** 计数 +1，首次写入时设置 TTL（滚动窗口），语义同评论防刷 */
    private long incrWithTtl(String key, int ttlSeconds) {
        return redisClient.open1(s -> {
            long count = s.key(key).incr();
            if (count == 1) {
                s.key(key).expire(ttlSeconds);
            }
            return count;
        });
    }
}
