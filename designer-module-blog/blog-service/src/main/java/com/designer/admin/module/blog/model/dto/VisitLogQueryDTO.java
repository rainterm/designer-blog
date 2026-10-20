package com.designer.admin.module.blog.model.dto;

import com.designer.admin.common.core.PageQuery;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 访问记录分页查询入参。
 *
 * <p>时间用 {@code yyyy-MM-dd} 字符串接收，和系统模块的日志查询保持一致：
 * 前端日期选择器直接给字符串，服务端再转成 {@link java.time.LocalDate}，
 * 避免不同序列化器对日期格式理解不一致。</p>
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class VisitLogQueryDTO extends PageQuery {

    @ApiModelProperty("关键字，匹配访问路径 / IP / 归属地")
    private String keyword;

    @ApiModelProperty("页面类型：home / post / tag / category / other，为空表示不限")
    private String pageType;

    @ApiModelProperty("设备：PC / MOBILE / TABLET / BOT / OTHER，为空表示不限")
    private String device;

    @ApiModelProperty("来源类型：direct / search / external，为空表示不限")
    private String refererType;

    @ApiModelProperty("访问日期起，格式 yyyy-MM-dd")
    private String beginDate;

    @ApiModelProperty("访问日期止，格式 yyyy-MM-dd")
    private String endDate;
}
