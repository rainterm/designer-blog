package com.designer.admin.module.blog.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.R;
import com.designer.admin.module.blog.model.vo.VisitOverviewVO;
import com.designer.admin.module.blog.service.BlogVisitService;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;

/**
 * 博客访问统计（后台）
 */
@Controller
@Mapping("/admin-api/blog/visit")
public class BlogVisitController {

    @Inject
    private BlogVisitService visitService;

    /**
     * 统计概览：概览数字 + 趋势 / 分类 / 来源 / 设备 / 地域。
     *
     * @param days 统计最近多少天（含今天），默认 30，最大 365
     */
    @Get
    @Mapping("/overview")
    @SaCheckPermission("blog:visit:query")
    public R<VisitOverviewVO> overview(Integer days) {
        return R.ok(visitService.overview(days));
    }
}
