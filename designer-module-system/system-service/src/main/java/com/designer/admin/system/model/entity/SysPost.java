package com.designer.admin.system.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.designer.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.noear.snack4.annotation.ONodeAttr;

/**
 * 岗位（全局字典；部门通过用户所属部门关联）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_post")
public class SysPost extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 岗位名称（如系长/科长/部长） */
    private String postName;

    /** 岗位编码（全局唯一，如 KE_ZHANG） */
    private String postCode;

    /** 职级，数值越大越高 */
    private Integer postLevel;

    /** 显示顺序 */
    private Integer sort;

    /** 1正常 0停用 */
    private Integer status;

    /** 备注 */
    private String remark;

    @TableLogic
    @ONodeAttr(ignore = true)
    private Integer deleted;
}
