package com.designer.admin.system.model.dto;

import com.designer.admin.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件分页查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FileQueryDTO extends PageQuery {

    /** 原始文件名（模糊） */
    private String originalName;

    /** 业务子目录 */
    private String bizDir;

    /** 存储类型：local / s3 */
    private String storageType;

    /** 扩展名 */
    private String extension;

    /** 创建人 */
    private String createBy;

    /** 创建时间范围，格式 yyyy-MM-dd */
    private String beginTime;
    private String endTime;
}
