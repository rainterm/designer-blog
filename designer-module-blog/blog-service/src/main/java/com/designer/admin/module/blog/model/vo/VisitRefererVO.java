package com.designer.admin.module.blog.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 来源域名聚合项（同时用作 Mapper 的查询结果载体）。
 *
 * <p>{@code host} 为空串表示直接访问 / 站内跳转，由前端展示成「(直接访问)」。</p>
 *
 * @author 雨夜终端
 */
@Data
public class VisitRefererVO implements Serializable {

    /** 来源域名，空串代表直接访问 */
    private String host;

    /** 来源类型：direct / search / external */
    private String type;

    /** 访问次数 */
    private Long value;
}
