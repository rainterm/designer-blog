package com.designer.admin.module.blog.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用「名称 - 数量」聚合项：分类分布 / 设备占比 / 地域分布共用。
 *
 * @author 雨夜终端
 */
@Data
public class VisitNameValueVO implements Serializable {

    /** 名称：分类名、设备码（PC/MOBILE/TABLET/OTHER）、省份 */
    private String name;

    /** 访问次数 */
    private Long value;
}
