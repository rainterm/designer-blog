package com.designer.admin.system.service;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.designer.admin.common.constant.SystemConstants;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.framework.captcha.CaptchaVerifier;
import com.designer.admin.framework.satoken.LoginHelper;
import com.designer.admin.framework.satoken.LoginUser;
import com.designer.admin.system.model.dto.LoginDTO;
import com.designer.admin.system.model.vo.CaptchaVO;
import com.designer.admin.system.model.vo.LoginVO;
import com.designer.admin.system.model.vo.RouterVO;
import com.designer.admin.system.model.vo.UserInfoVO;
import com.designer.admin.system.model.entity.SysLoginLog;
import com.designer.admin.system.model.entity.SysUser;
import com.designer.admin.system.mapper.SysMenuMapper;
import com.designer.admin.system.mapper.SysRoleMapper;
import com.designer.admin.system.mapper.SysUserMapper;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.core.handle.Context;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 认证服务：登录 / 登出 / 当前用户信息 / 动态路由
 */
@Component
public class AuthService {

    @Db
    private SysUserMapper userMapper;
    @Db
    private SysRoleMapper roleMapper;
    @Db
    private SysMenuMapper menuMapper;
    @Inject
    private SysMenuService menuService;
    @Inject
    private SysLoginLogService loginLogService;
    @Inject
    private CaptchaVerifier captchaVerifier;
    @Inject
    private LoginAttemptService loginAttemptService;
    @Inject
    private SysConfigService configService;

    /**
     * 账号密码登录：人机验证、失败锁定、BCrypt 校验、记录登录日志、写入 Sa-Token session。
     */
    public LoginVO login(LoginDTO dto, Context ctx) {
        String ip = ctx == null ? "" : ctx.realIp();
        String userAgent = ctx == null ? null : ctx.userAgent();
        String username = dto.getUsername();

        // 先问「要不要验」，再决定是否消费凭据：凭据一次性，
        // 对不需要验证的账号调校验会把它白白用掉（见 LoginAttemptService 的类注释）
        if (loginAttemptService.needCaptcha(username)) {
            captchaVerifier.assertVerified(dto.getCaptchaVerification());
        }
        loginAttemptService.assertNotLocked(username);

        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));

        if (user == null || !BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            recordLoginLog(username, ip, userAgent, 0, "账号或密码错误");
            try {
                loginAttemptService.recordLoginFail(username);
            } catch (BizException lockEx) {
                recordLoginLog(username, ip, userAgent, 0, lockEx.getMessage());
                throw lockEx;
            }
            throw new BizException("账号或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != SystemConstants.STATUS_ENABLE) {
            recordLoginLog(username, ip, userAgent, 0, "账号已停用");
            throw new BizException("账号已停用，请联系管理员");
        }

        loginAttemptService.clearLoginFail(username);
        long tokenTimeout = configService.getTokenTimeoutSeconds();
        long activeTimeout = configService.getTokenActiveTimeoutSeconds();
        SaLoginParameter loginParam = new SaLoginParameter().setTimeout(tokenTimeout);
        if (activeTimeout > 0) {
            loginParam.setActiveTimeout(activeTimeout);
        }
        StpUtil.login(user.getId(), loginParam);

        List<String> roles = roleMapper.selectRoleKeysByUserId(user.getId());
        boolean superAdmin = roleMapper.selectIsSuperByUserId(user.getId());

        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setNickname(user.getNickname());
        loginUser.setAvatar(user.getAvatar());
        loginUser.setRoles(roles);
        loginUser.setSuperAdmin(superAdmin);
        StpUtil.getSession().set(LoginHelper.LOGIN_USER_KEY, loginUser);

        // 写入 token 会话，供在线用户列表展示
        StpUtil.getTokenSession().set(OnlineUserService.TOKEN_LOGIN_IP, ip);
        StpUtil.getTokenSession().set(OnlineUserService.TOKEN_BROWSER, parseBrowser(userAgent));
        StpUtil.getTokenSession().set(OnlineUserService.TOKEN_OS, parseOs(userAgent));
        StpUtil.getTokenSession().set(OnlineUserService.TOKEN_LOGIN_TIME, System.currentTimeMillis());

        recordLoginLog(user.getUsername(), ip, userAgent, 1, "登录成功");
        return toLoginVo(StpUtil.getTokenValue());
    }

    /** 由已有 token 组装登录结果（SSO 换票复用） */
    public LoginVO toLoginVo(String token) {
        if (token == null || token.isBlank()) {
            throw new BizException("登录状态已失效，请重新登录");
        }
        Object loginId = StpUtil.getLoginIdByToken(token);
        if (loginId == null) {
            throw new BizException("登录状态已失效，请重新登录");
        }
        Long userId = Long.parseLong(String.valueOf(loginId));
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在或已被删除");
        }
        List<String> roles = roleMapper.selectRoleKeysByUserId(user.getId());
        LoginVO vo = new LoginVO();
        vo.setTokenName(StpUtil.getTokenName());
        vo.setToken(token);
        vo.setExpiresIn(StpUtil.getTokenTimeout(token));
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setRoles(roles);
        return vo;
    }

    /**
     * 登录前询问该账号是否需要人机验证。
     *
     * <p>验证码本身由 AJ-Captcha 的 {@code /captcha/get} 下发（与博客前台评论共用同一套端点），
     * 这里只回答「要不要」，前端据此决定是否展示滑块。</p>
     */
    public CaptchaVO captcha(String username) {
        CaptchaVO vo = new CaptchaVO();
        vo.setEnabled(loginAttemptService.needCaptcha(username));
        return vo;
    }

    public void logout() {
        StpUtil.logout();
    }

    /** 当前登录用户信息（含权限标识） */
    public UserInfoVO getUserInfo() {
        Long userId = LoginHelper.getUserId();
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在或已被删除");
        }
        List<String> roles = roleMapper.selectRoleKeysByUserId(userId);
        boolean superAdmin = roleMapper.selectIsSuperByUserId(userId);

        UserInfoVO vo = new UserInfoVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setRoles(roles);
        vo.setSuperAdmin(superAdmin);
        vo.setPerms(superAdmin ? List.of("*") : menuMapper.selectPermsByUserId(userId));
        return vo;
    }

    /** 当前登录用户的动态菜单路由 */
    public List<RouterVO> getRouters() {
        Long userId = LoginHelper.getUserId();
        boolean superAdmin = roleMapper.selectIsSuperByUserId(userId);
        return menuService.buildRouters(userId, superAdmin);
    }

    private void recordLoginLog(String username, String ip, String userAgent, int status, String msg) {
        SysLoginLog log = new SysLoginLog();
        log.setUsername(username);
        log.setIp(ip);
        log.setBrowser(parseBrowser(userAgent));
        log.setOs(parseOs(userAgent));
        log.setStatus(status);
        log.setMsg(msg);
        log.setLoginTime(LocalDateTime.now());
        loginLogService.recordAsync(log);
    }

    private String parseBrowser(String ua) {
        if (ua == null) {
            return "未知";
        }
        if (ua.contains("Edg/")) return "Edge";
        if (ua.contains("Chrome/")) return "Chrome";
        if (ua.contains("Firefox/")) return "Firefox";
        if (ua.contains("Safari/")) return "Safari";
        return "其它";
    }

    private String parseOs(String ua) {
        if (ua == null) {
            return "未知";
        }
        if (ua.contains("Windows")) return "Windows";
        if (ua.contains("Mac OS X") || ua.contains("Macintosh")) return "macOS";
        if (ua.contains("Android")) return "Android";
        if (ua.contains("iPhone") || ua.contains("iPad")) return "iOS";
        if (ua.contains("Linux")) return "Linux";
        return "其它";
    }
}
