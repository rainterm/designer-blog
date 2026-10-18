package com.designer.admin.module.blog.model.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 发布 / 下线文章。
 *
 * <p>请求体可以为空：缺省视为发布。前端「发布」与「下线」两个按钮共用同一入口，
 * 通过 {@code publish} 布尔值区分。</p>
 *
 * @author 雨夜终端
 */
@Data
public class PostPublishDTO {

    /** 是否发布：true 发布，false 下线；缺省为 true */
    @ApiModelProperty(value = "是否发布：true 发布，false 下线，缺省 true", example = "true")
    private Boolean publish;

    /**
     * 是否发布：缺省视为发布。
     *
     * @return 需要发布时返回 true
     */
    public boolean shouldPublish() {
        return publish == null || publish;
    }
}
