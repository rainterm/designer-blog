package com.designer.admin.system.model.vo;

import lombok.Data;

import java.util.List;

/**
 * 可注册的存储类型（供前端动态表单）
 */
@Data
public class StorageTypeVO {

    private String type;
    private String label;
    private List<ConfigFieldMeta> fields;
    /** 私有代理下载前缀（来自 app.yml） */
    private String defaultUrlPrefix;
    /** 公开代理下载前缀（来自 app.yml） */
    private String publicUrlPrefix;
}
