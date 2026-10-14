package com.designer.admin.system.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.designer.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统参数配置（键值对）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_config")
public class SysConfig extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 参数名称 */
    private String configName;

    /** 参数键名（全局唯一） */
    private String configKey;

    /** 参数键值 */
    private String configValue;

    /** 系统内置：Y是 N否（内置不可删除） */
    private String configType;

    /** 备注 */
    private String remark;
}
