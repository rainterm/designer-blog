package com.designer.admin.system.service;

import cn.dev33.satoken.stp.StpUtil;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.system.model.vo.LoginVO;
import com.designer.admin.system.model.vo.SsoTicketVO;
import org.noear.redisx.RedisClient;
import org.noear.solon.Solon;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import java.util.UUID;

/**
 * 跨应用单点登录：已登录端签发一次性票据，目标端换取同一 Sa-Token。
 */
@Component
public class SsoService {

    private static final String TICKET_KEY = "sso:ticket:";
    private static final int DEFAULT_TTL = 60;

    @Inject
    private RedisClient redisClient;
    @Inject
    private AuthService authService;
    @Inject
    private SysSsoClientService ssoClientService;

    public SsoTicketVO issueTicket() {
        return issueTicket(null);
    }

    /**
     * @param redirect 可选；传入时校验 Origin 是否在启用的 SSO 客户端白名单中
     */
    public SsoTicketVO issueTicket(String redirect) {
        StpUtil.checkLogin();
        if (redirect != null && !redirect.isBlank()) {
            ssoClientService.assertAllowedReturnUrl(redirect);
        }
        String token = StpUtil.getTokenValue();
        if (token == null || token.isBlank()) {
            throw new BizException("未登录");
        }
        int ttl = Math.max(15, Solon.cfg().getInt("designer.sso.ticket-ttl", DEFAULT_TTL));
        String ticket = UUID.randomUUID().toString().replace("-", "");
        redisClient.open0(s -> s.key(TICKET_KEY + ticket).expire(ttl).set(token));
        SsoTicketVO vo = new SsoTicketVO();
        vo.setTicket(ticket);
        vo.setExpiresIn(ttl);
        return vo;
    }

    public LoginVO exchange(String ticket) {
        if (ticket == null || ticket.isBlank()) {
            throw new BizException("缺少登录票据");
        }
        String key = TICKET_KEY + ticket.trim();
        String token = redisClient.open1(s -> {
            String val = s.key(key).get();
            if (val != null && !val.isBlank()) {
                s.key(key).delete();
            }
            return val;
        });
        if (token == null || token.isBlank()) {
            throw new BizException("登录票据无效或已过期");
        }
        return authService.toLoginVo(token);
    }
}
