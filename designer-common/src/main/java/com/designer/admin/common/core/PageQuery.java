package com.designer.admin.common.core;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

/**
 * 分页查询参数基类，业务查询 DTO 继承它即可获得分页能力
 */
@Data
public class PageQuery {

    /** 页码，从 1 开始 */
    private long pageNum = 1;

    /** 每页条数 */
    private long pageSize = 10;

    public long getPageNum() {
        return pageNum < 1 ? 1 : pageNum;
    }

    public long getPageSize() {
        if (pageSize < 1) {
            return 10;
        }
        // 限制单页最大条数，避免恶意查询
        return Math.min(pageSize, 500);
    }

    /** 转换为 MyBatis-Plus 分页对象 */
    public <T> Page<T> toPage() {
        return new Page<>(getPageNum(), getPageSize());
    }
}
