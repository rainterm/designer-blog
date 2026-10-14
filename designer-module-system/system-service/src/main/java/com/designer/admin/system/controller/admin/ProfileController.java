package com.designer.admin.system.controller.admin;

import com.designer.admin.common.core.R;
import com.designer.admin.framework.satoken.LoginHelper;
import com.designer.admin.system.service.AuthService;
import com.designer.admin.system.service.SysUserService;
import com.designer.admin.system.model.vo.UserInfoVO;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Put;
import org.noear.solon.validation.annotation.NotBlank;
import org.noear.solon.validation.annotation.Valid;

/**
 * 个人中心：资料查看/修改、改密
 */
@Valid
@Controller
@Mapping("/admin-api/system/profile")
public class ProfileController {

    @Inject
    private AuthService authService;

    @Inject
    private SysUserService userService;

    /**
     * 获取当前登录用户信息（复用 AuthService.getUserInfo）
     */
    @Get
    @Mapping("")
    public R<UserInfoVO> profile() {
        return R.ok(authService.getUserInfo());
    }

    /**
     * 修改当前用户的昵称、头像、邮箱、手机号
     */
    @Put
    @Mapping("/update")
    public R<Void> update(String nickname, String avatar, String email, String phone) {
        userService.updateProfile(LoginHelper.getUserId(), nickname, avatar, email, phone);
        return R.ok();
    }

    /**
     * 修改当前用户自己的密码，需校验原密码
     */
    @Put
    @Mapping("/password")
    public R<Void> changePassword(@NotBlank(message = "原密码不能为空") String oldPassword,
                                  @NotBlank(message = "新密码不能为空") String newPassword) {
        userService.changePassword(LoginHelper.getUserId(), oldPassword, newPassword);
        return R.ok();
    }
}