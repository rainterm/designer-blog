package com.designer.admin.module.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.common.core.ResultCode;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.framework.client.ClientIpUtil;
import com.designer.admin.framework.client.IpRegionResolver;
import com.designer.admin.framework.dict.DictLookup;
import com.designer.admin.framework.redis.RedisCounter;
import com.designer.admin.framework.redis.RedisKeyConsts;
import com.designer.admin.module.blog.constant.BlogDictConsts;
import com.designer.admin.module.blog.constant.BlogRedisKeyConsts;
import com.designer.admin.module.blog.constant.BlogVisitConsts;
import com.designer.admin.module.blog.mapper.BlogPostMapper;
import com.designer.admin.module.blog.mapper.BlogVisitMapper;
import com.designer.admin.module.blog.model.dto.VisitLogQueryDTO;
import com.designer.admin.module.blog.model.dto.VisitReportDTO;
import com.designer.admin.module.blog.model.entity.BlogPost;
import com.designer.admin.module.blog.model.entity.BlogVisitLog;
import com.designer.admin.module.blog.model.vo.VisitLogVO;
import com.designer.admin.module.blog.model.vo.VisitNameValueVO;
import com.designer.admin.module.blog.model.vo.VisitOverviewVO;
import com.designer.admin.module.blog.model.vo.VisitSummaryVO;
import com.designer.admin.module.blog.model.vo.VisitTrendVO;
import com.designer.admin.module.blog.service.BlogViewCounter;
import com.designer.admin.module.blog.service.BlogVisitService;
import com.designer.admin.module.blog.util.HashUtil;
import com.designer.admin.module.blog.util.RefererUtil;
import com.designer.admin.module.blog.util.UserAgentUtil;
import com.designer.admin.module.blog.util.VisitPathUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.core.handle.Context;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 博客访问统计。
 *
 * <p>写入侧（{@link #addVisit}）由前台匿名接口触发，**永不抛异常**：
 * 埋点是附加能力，任何失败都不该让访客的页面报错。
 * 读取侧（{@link #getOverview}）供后台统计页使用，一次返回全部图表数据。</p>
 *
 * @author 雨夜终端
 */
@Component
@Slf4j
public class BlogVisitServiceImpl implements BlogVisitService {

    /** 访问记录筛选用的日期格式，与前端日期选择器一致 */
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Db
    private BlogVisitMapper visitMapper;
    @Db
    private BlogPostMapper postMapper;
    @Inject
    private IpRegionResolver ipRegionResolver;
    @Inject
    private RedisCounter redisCounter;
    @Inject
    private BlogViewCounter viewCounter;
    @Inject
    private DictLookup dictLookup;

    @Override
    public void addVisit(VisitReportDTO dto) {
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
        if (redisCounter.incrWithTtl(BlogRedisKeyConsts.VISIT_DEDUP_PREFIX + visitorKey
                + RedisKeyConsts.SEPARATOR + parsed.path(), BlogVisitConsts.DEDUP_TTL_SECONDS) > 1) {
            return;
        }
        if (redisCounter.incrWithTtl(BlogRedisKeyConsts.VISIT_QUOTA_PREFIX + ip, BlogVisitConsts.DAY_SECONDS) > BlogVisitConsts.DAILY_QUOTA) {
            // 超配额静默丢弃：对访客毫无感知，也不会污染统计
            return;
        }

        String device = UserAgentUtil.device(ua);
        recordRead(postId, device);

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
        entity.setDevice(device);
        entity.setRefererHost(refererHost);
        entity.setRefererType(RefererUtil.type(refererHost).getCode());
        entity.setUa(ua);
        visitMapper.insert(entity);
    }

    @Override
    public VisitOverviewVO getOverview(Integer days) {
        int span = normalizeDays(days);
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(span - 1L);

        VisitOverviewVO vo = new VisitOverviewVO();
        vo.setDays(span);
        vo.setSummary(buildSummary(from, today));
        // 趋势补齐空缺日期：图表缺日会断线，视觉上像丢数据
        vo.setTrend(fillMissingDays(visitMapper.selectTrend(from), from, today));
        vo.setCategories(visitMapper.selectCategoryRank(from, BlogVisitConsts.TOP_SIZE));
        vo.setReferers(visitMapper.selectRefererRank(from, BlogVisitConsts.TOP_SIZE));
        vo.setDevices(withDeviceLabels(visitMapper.selectDeviceRank(from),
                dictLookup.labelMap(BlogDictConsts.VISIT_DEVICE)));
        vo.setRegions(visitMapper.selectRegionRank(from, BlogVisitConsts.TOP_SIZE));
        return vo;
    }

    @Override
    public IPage<VisitLogVO> listLogs(VisitLogQueryDTO query) {
        LocalDate begin = parseDay(query.getBeginDate(), "开始日期");
        LocalDate end = parseDay(query.getEndDate(), "结束日期");
        if (begin != null && end != null && begin.isAfter(end)) {
            throw new BizException(ResultCode.BAD_REQUEST, "开始日期不能晚于结束日期");
        }
        String keyword = blankToNull(query.getKeyword());
        // 条件为 false 时 MP 仍会先求值参数，日期解析必须放在 wrapper 之外
        LambdaQueryWrapper<BlogVisitLog> wrapper = new LambdaQueryWrapper<BlogVisitLog>()
                .ge(begin != null, BlogVisitLog::getVisitDate, begin)
                .le(end != null, BlogVisitLog::getVisitDate, end)
                .eq(notBlank(query.getPageType()), BlogVisitLog::getPageType, query.getPageType())
                .eq(notBlank(query.getDevice()), BlogVisitLog::getDevice, query.getDevice())
                .eq(notBlank(query.getRefererType()), BlogVisitLog::getRefererType, query.getRefererType())
                .and(keyword != null, w -> w
                        .like(BlogVisitLog::getPath, keyword)
                        .or()
                        .like(BlogVisitLog::getIp, keyword)
                        .or()
                        .like(BlogVisitLog::getIpLocation, keyword))
                .orderByDesc(BlogVisitLog::getVisitDate)
                .orderByDesc(BlogVisitLog::getId);
        // 字典映射在循环外取一次：@Cache 在 labelMap 上，一页数据只读一次缓存
        Map<String, String> pageTypes = dictLookup.labelMap(BlogDictConsts.VISIT_PAGE_TYPE);
        Map<String, String> devices = dictLookup.labelMap(BlogDictConsts.VISIT_DEVICE);
        Map<String, String> refererTypes = dictLookup.labelMap(BlogDictConsts.VISIT_REFERER_TYPE);
        return visitMapper.selectPage(query.toPage(), wrapper)
                .convert(e -> toLogVO(e, pageTypes, devices, refererTypes));
    }

    @Override
    public int removeOlderThan(Integer days) {
        int keepDays = (days == null || days < 1) ? BlogVisitConsts.DEFAULT_KEEP_DAYS : days;
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
        Map<LocalDate, VisitTrendVO> byDate = new LinkedHashMap<>();
        for (VisitTrendVO row : rows) {
            byDate.put(row.getDate(), row);
        }
        List<VisitTrendVO> filled = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            VisitTrendVO row = byDate.get(d);
            if (row == null) {
                row = new VisitTrendVO();
                row.setDate(d);
                row.setPv(0L);
                row.setUv(0L);
            }
            filled.add(row);
        }
        return filled;
    }

    /**
     * 设备码转中文标签。
     *
     * <p>库里存的是 {@code PC / MOBILE / TABLET / BOT / OTHER} 这类语言无关的码，
     * 文案取自数据字典 {@code blog_visit_device}，运营改文案不用发版。</p>
     */
    private List<VisitNameValueVO> withDeviceLabels(List<VisitNameValueVO> rows,
                                                    Map<String, String> deviceLabels) {
        for (VisitNameValueVO row : rows) {
            row.setName(label(deviceLabels, row.getName()));
        }
        return rows;
    }

    /** 明细分页的实体 → 展示对象：码值在这里统一查字典转中文，前端不再维护映射 */
    private VisitLogVO toLogVO(BlogVisitLog entity,
                               Map<String, String> pageTypes,
                               Map<String, String> deviceLabels,
                               Map<String, String> refererTypes) {
        VisitLogVO vo = new VisitLogVO();
        vo.setId(entity.getId());
        vo.setVisitDate(entity.getVisitDate());
        vo.setPageType(entity.getPageType());
        vo.setPageTypeLabel(label(pageTypes, entity.getPageType()));
        vo.setPath(entity.getPath());
        vo.setPostId(entity.getPostId());
        vo.setIp(entity.getIp());
        vo.setIpLocation(entity.getIpLocation());
        vo.setDevice(entity.getDevice());
        vo.setDeviceLabel(label(deviceLabels, entity.getDevice()));
        vo.setRefererHost(entity.getRefererHost());
        vo.setRefererType(entity.getRefererType());
        vo.setRefererTypeLabel(label(refererTypes, entity.getRefererType()));
        vo.setUa(entity.getUa());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    /**
     * 字典里取文案，缺项时回退成码值本身。
     *
     * <p>字典项被停用 / 被误删 / 历史数据里有字典中不存在的码，都不该让列表出现空文案 ——
     * 显示 {@code external} 也好过显示一个空白单元格，至少能看出是哪一类。</p>
     */
    private static String label(Map<String, String> labels, String code) {
        if (code == null || code.isEmpty()) {
            return "";
        }
        String text = labels.get(code);
        return text == null || text.isEmpty() ? code : text;
    }

    /** 解析 yyyy-MM-dd；空值返回 null，格式错误给出可读提示而不是 500 */
    private LocalDate parseDay(String value, String fieldName) {
        if (!notBlank(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim(), DAY);
        } catch (DateTimeParseException e) {
            throw new BizException(ResultCode.BAD_REQUEST, fieldName + "格式应为 yyyy-MM-dd");
        }
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private String blankToNull(String s) {
        return notBlank(s) ? s.trim() : null;
    }

    private int normalizeDays(Integer days) {
        if (days == null || days < 1) {
            return BlogVisitConsts.DEFAULT_DAYS;
        }
        return Math.min(days, BlogVisitConsts.MAX_DAYS);
    }

    /**
     * 累加一篇文章的阅读数。
     *
     * <p>刻意挂在**浏览器侧**的埋点链路上，而不是文章详情接口：SSR 取数走内网，
     * 后端看到的 IP 是 blog 容器、UA 是 Node 的 fetch —— 既无法按访客去重，
     * 也认不出爬虫，爬虫抓一次阅读数就涨一次。放在这里，前面的 60 秒去重
     * 与配额检查都是白拿的，且爬虫（不执行 JS）根本不会走到这一步。</p>
     *
     * <p>调用点在去重与配额**之后**，所以刷新页面不会重复计数。</p>
     */
    private void recordRead(Long postId, String device) {
        if (postId == null || UserAgentUtil.BOT.equals(device)) {
            return;
        }
        try {
            viewCounter.record(postId);
        } catch (Exception e) {
            // 与埋点同策略：计数属于附加能力，失败不能影响访客请求
            log.warn("[blog-visit] 阅读数累加失败 postId={}：{}", postId, e.getMessage());
        }
    }
}
