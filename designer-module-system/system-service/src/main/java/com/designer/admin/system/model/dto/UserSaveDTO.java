package com.designer.admin.system.model.dto;

import com.designer.admin.common.annotation.Sensitive;
import com.designer.admin.common.annotation.SensitiveType;
import com.designer.admin.common.constant.PasswordPolicy;
import lombok.Data;
import org.noear.solon.validation.annotation.Length;
import org.noear.solon.validation.annotation.NotBlank;
import org.noear.solon.validation.annotation.Pattern;

import java.util.List;

/**
 * 用户保存（新增/修改通用）
 */
@Data
public class UserSaveDTO {

    private Long id;

    @NotBlank(message = "账号不能为空")
    private String username;

    /**
     * 新增必填；修改为空表示不改密码。
     *
     * <p>空值放行是两个校验注解的固有语义（LengthValidator / PatternValidator 都是
     * 「值为空即算通过」），正好对上「留空表示不修改」；只有真填了才按
     * {@link PasswordPolicy} 卡长度与复杂度。</p>
     */
    @Sensitive
    @Length(min = PasswordPolicy.MIN_LENGTH, max = PasswordPolicy.MAX_LENGTH,
            message = PasswordPolicy.LENGTH_MESSAGE)
    @Pattern(value = PasswordPolicy.STRENGTH_REGEXP, message = PasswordPolicy.COMPLEXITY_MESSAGE)
    private String password;

    @NotBlank(message = "昵称不能为空")
    private String nickname;

    private String avatar;

    @Sensitive(type = SensitiveType.EMAIL)
    private String email;

    @Sensitive(type = SensitiveType.PHONE)
    private String phone;
    private Integer status;
    private String remark;

    /** 所属部门 */
    private Long deptId;

    /** 关联角色ID */
    private List<Long> roleIds;

    /** 关联岗位ID */
    private List<Long> postIds;
}
