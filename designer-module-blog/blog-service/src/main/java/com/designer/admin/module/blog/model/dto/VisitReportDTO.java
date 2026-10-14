package com.designer.admin.module.blog.model.dto;

import lombok.Data;

/**
 * 前台上报的访问埋点。
 *
 * <p><b>只接收路径</b>：IP / UA / 来源 / 设备 / 归属分类全部由服务端从请求推导，
 * 前端无法伪造（否则统计可被随意污染）。</p>
 */
@Data
public class VisitReportDTO {

    /** 访问路径，如 {@code /post/markdown-guide}；缺失时回退为请求自身路径 */
    private String path;
}
