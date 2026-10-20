package com.designer.admin.module.blog.controller.admin;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.Result;
import com.designer.admin.module.blog.model.dto.VisitLogQueryDTO;
import com.designer.admin.module.blog.model.vo.VisitLogVO;
import com.designer.admin.module.blog.model.vo.VisitOverviewVO;
import com.designer.admin.module.blog.service.BlogVisitService;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;

/**
 * 博客访问统计（后台）
 *
 * @author 雨夜终端
 */
@Controller
@Mapping("/admin-api/blog/visit")
@Api(tags = "访问统计")
public class BlogVisitController {

    @Inject
    private BlogVisitService visitService;

    @Get
    @Mapping("/overview")
    @SaCheckPermission("blog:visit:query")
    @ApiOperation(value = "获取访问统计概览")
    public Result<VisitOverviewVO> getOverview(Integer days) {
        return Result.ok(visitService.getOverview(days));
    }

    @Get
    @Mapping("/page")
    @SaCheckPermission("blog:visit:query")
    @ApiOperation(value = "分页查询访问记录")
    public Result<PageResult<VisitLogVO>> page(VisitLogQueryDTO query) {
        return Result.ok(PageResult.of(visitService.listLogs(query)));
    }
}
