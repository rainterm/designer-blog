package com.designer.admin.system.controller.admin;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import com.designer.admin.common.core.R;
import com.designer.admin.system.model.dto.LoginDTO;
import com.designer.admin.system.model.dto.SsoExchangeDTO;
import com.designer.admin.system.model.dto.SsoTicketDTO;
import com.designer.admin.system.model.vo.CaptchaVO;
import com.designer.admin.system.model.vo.LoginVO;
import com.designer.admin.system.model.vo.RouterVO;
import com.designer.admin.system.model.vo.SsoTicketVO;
import com.designer.admin.system.model.vo.UserInfoVO;
import com.designer.admin.system.service.AuthService;
import com.designer.admin.system.service.SsoService;
import org.noear.solon.annotation.Body;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Post;
import org.noear.solon.core.handle.Context;
import org.noear.solon.validation.annotation.Valid;

import java.util.List;

/**
 * 认证：登录、登出、当前用户、动态路由
 */
@Valid
@Controller
@Mapping("/admin-api/system/auth")
public class AuthController {

    @Inject
    private AuthService authService;
    @Inject
    private SsoService ssoService;

    /**
     * 获取登录验证码。未开启或该账号失败未达阈值时 enabled=false。
     *
     * @param username 登录账号（用于判断是否已达失败阈值）
     */
    @SaIgnore
    @Get
    @Mapping("/captcha")
    public R<CaptchaVO> captcha(String username) {
        return R.ok(authService.captcha(username));
    }

    /**
     * 账号密码登录，返回 token 与基础用户信息
     */
    @SaIgnore
    @Post
    @Mapping("/login")
    public R<LoginVO> login(@Body LoginDTO dto, Context ctx) {
        return R.ok(authService.login(dto, ctx));
    }

    /**
     * 注销当前 token
     */
    @Post
    @Mapping("/logout")
    public R<Void> logout() {
        StpUtil.logout();
        return R.ok();
    }

    /**
     * 获取当前登录用户信息（含角色与权限标识）
     */
    @Get
    @Mapping("/info")
    public R<UserInfoVO> info() {
        return R.ok(authService.getUserInfo());
    }

    /**
     * 获取当前用户可见的动态路由（用于前端注册路由与菜单）
     */
    @Get
    @Mapping("/routers")
    public R<List<RouterVO>> routers() {
        return R.ok(authService.getRouters());
    }

    /**
     * 已登录应用签发一次性票据，供其它前端换取同一 token。
     * body.redirect 可选：传入时按 SSO 客户端白名单校验回跳 Origin。
     */
    @Post
    @Mapping("/sso/ticket")
    public R<SsoTicketVO> ssoTicket(@Body SsoTicketDTO dto) {
        return R.ok(ssoService.issueTicket(dto == null ? null : dto.getRedirect()));
    }

    /**
     * 一次性票据换 token（免登录）
     */
    @SaIgnore
    @Post
    @Mapping("/sso/exchange")
    public R<LoginVO> ssoExchange(@Body SsoExchangeDTO dto) {
        return R.ok(ssoService.exchange(dto == null ? null : dto.getTicket()));
    }
}