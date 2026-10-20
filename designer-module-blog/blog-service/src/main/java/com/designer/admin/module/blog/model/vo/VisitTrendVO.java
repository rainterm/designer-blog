package com.designer.admin.module.blog.model.vo;

import com.designer.admin.common.core.DateTimePattern;
import lombok.Data;
import org.noear.snack4.annotation.ONodeAttr;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 按天的访问趋势点（同时用作 Mapper 的查询结果载体）。
 *
 * <p>日期保持 {@link LocalDate}：格式化交给序列化层（{@code @ONodeAttr}），
 * 不在 SQL 里调 {@code DATE_FORMAT} 之类的方言函数。</p>
 *
 * @author 雨夜终端
 */
@Data
public class VisitTrendVO implements Serializable {

    /** 日期，序列化为 yyyy-MM-dd */
    @ONodeAttr(format = DateTimePattern.DATE)
    private LocalDate date;

    /** 当日浏览量 */
    private Long pv;

    /** 当日独立访客数 */
    private Long uv;
}
