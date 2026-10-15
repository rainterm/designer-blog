package com.designer.admin.system.controller.admin;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import com.designer.admin.common.core.R;
import com.designer.admin.framework.ratelimit.RateLimit;
import com.designer.admin.framework.ratelimit.RateLimitDimension;
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
     * 获取登录验证码提示。未开启或该账号失败未达阈值时 enabled=false。
     *
     * <p>只返回「要不要验」，不返回图片：滑块由 AJ-Captcha 的 {@code /captcha/get}
     * 下发（与博客前台评论共用同一套端点）。</p>
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
     * 账号密码登录，返回 token 与基础用户信息。
     *
     * <p><b>限流按 IP</b>：账号维度的防线在 {@code LoginAttemptService}（连续失败触发验证码、
     * 达上限锁定），但它挡不住「换着用户名扫」——每个用户名只错一两次，
     * 既够不到验证码阈值也触发不了锁定。撞库/爆破正是这种形态，
     * 只有按来源 IP 计一份总额度才能挡住。</p>
     *
     * <p>两条规则按声明顺序校验，前一条挡住就不消耗后一条的额度：
     * 分钟级限突发，日级限慢速爆破（例如每 5 分钟试一次，单靠分钟规则永远拦不住）。</p>
     *
     * <p>阈值刻意偏宽松：同一出口 IP（公司 NAT）后可能坐着多个管理员，
     * 卡太紧会把他们一起误伤。真正的强约束仍交给账号锁定 —— 那里按账号、按失败次数精确计算。</p>
     *
     * <p>⚠ {@code @Valid} 参数校验是 Filter、先于本拦截器执行，
     * 所以「账号/密码为空」的请求会先被校验拦下、不消耗额度；能消耗额度的都是形态完整的登录尝试。</p>
     */
    @SaIgnore
    @Post
    @Mapping("/login")
    @RateLimit(name = "system:login", dimension = RateLimitDimension.IP, rules = {
            @RateLimit.Rule(count = 10, window = 60, message = "登录尝试过于频繁，请 {} 秒后再试"),
            @RateLimit.Rule(count = 100, window = 24 * 60 * 60,
                    message = "24 小时内登录尝试次数过多，请稍后再试")
    })
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