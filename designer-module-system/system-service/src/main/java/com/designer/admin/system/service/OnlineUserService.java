package com.designer.admin.system.service;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.session.SaTerminalInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.framework.satoken.LoginHelper;
import com.designer.admin.framework.satoken.LoginUser;
import com.designer.admin.system.model.dto.OnlineUserQueryDTO;
import com.designer.admin.system.model.vo.OnlineUserVO;
import org.noear.redisx.RedisClient;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 在线用户：遍历 Account-Session 终端列表 + 强退
 */
@Component
public class OnlineUserService {
    public static final String TOKEN_LOGIN_IP = "loginIp";
    public static final String TOKEN_BROWSER = "browser";
    public static final String TOKEN_OS = "os";
    public static final String TOKEN_LOGIN_TIME = "loginTime";


    @Inject
    private RedisClient redisClient;

    /**
     * 在线用户分页。
     * 会话数据存在 Redis，无法交给数据库分页，故先汇总过滤再内存切片。
     */
    public IPage<OnlineUserVO> page(OnlineUserQueryDTO query) {
        List<OnlineUserVO> all = list(query == null ? null : query.getUsername(),
                query == null ? null : query.getIp());
        long total = all.size();
        long pageNum = query == null ? 1 : query.getPageNum();
        long pageSize = query == null ? 10 : query.getPageSize();
        long from = Math.max(0, (pageNum - 1) * pageSize);
        long to = Math.min(total, from + pageSize);
        List<OnlineUserVO> slice = from >= total ? List.of() : all.subList((int) from, (int) to);
        Page<OnlineUserVO> page = new Page<>(pageNum, pageSize, total);
        page.setRecords(slice);
        return page;
    }

    public List<OnlineUserVO> list(String username, String ip) {
        String sessionPrefix = StpUtil.getTokenName() + ":" + StpUtil.getLoginType() + ":session:";
        Set<String> sessionKeys;
        try {
            sessionKeys = redisClient.open1(s -> s.keys(sessionPrefix + "*"));
        } catch (Exception e) {
            throw new BizException("读取在线会话失败: " + e.getMessage());
        }
        if (sessionKeys == null || sessionKeys.isEmpty()) {
            return List.of();
        }

        List<OnlineUserVO> result = new ArrayList<>();
        Map<String, LoginUser> userCache = new HashMap<>();

        for (String sessionKey : sessionKeys) {
            String loginId = sessionKey.substring(sessionPrefix.length());
            if (loginId.isBlank()) {
                continue;
            }
            SaSession session;
            try {
                session = StpUtil.getSessionByLoginId(loginId, false);
            } catch (Exception e) {
                continue;
            }
            if (session == null) {
                continue;
            }

            LoginUser loginUser = userCache.computeIfAbsent(loginId, id -> {
                Object lu = session.get(LoginHelper.LOGIN_USER_KEY);
                return lu instanceof LoginUser u ? u : null;
            });

            String uname = loginUser != null ? loginUser.getUsername() : loginId;
            String nickname = loginUser != null ? loginUser.getNickname() : "";
            if (notBlank(username) && (uname == null || !uname.contains(username))) {
                continue;
            }

            List<SaTerminalInfo> terminals = session.getTerminalList();
            if (terminals == null || terminals.isEmpty()) {
                continue;
            }

            for (SaTerminalInfo terminal : terminals) {
                if (terminal == null || !notBlank(terminal.getTokenValue())) {
                    continue;
                }
                String token = terminal.getTokenValue();
                // 确认 token 仍有效（已被强退/过期的终端跳过）
                Object tokenLoginId;
                try {
                    tokenLoginId = StpUtil.getLoginIdByToken(token);
                } catch (Exception e) {
                    continue;
                }
                if (tokenLoginId == null || !String.valueOf(tokenLoginId).equals(loginId)) {
                    continue;
                }

                OnlineUserVO vo = new OnlineUserVO();
                vo.setTokenId(token);
                try {
                    vo.setUserId(Long.parseLong(loginId));
                } catch (NumberFormatException e) {
                    continue;
                }
                vo.setUsername(uname);
                vo.setNickname(nickname);

                fillTokenMeta(vo, token, terminal);

                if (notBlank(ip) && (vo.getIp() == null || !vo.getIp().contains(ip))) {
                    continue;
                }
                try {
                    vo.setTimeout(StpUtil.getTokenTimeout(token));
                } catch (Exception e) {
                    vo.setTimeout(-1L);
                }
                result.add(vo);
            }
        }

        result.sort(Comparator.comparing(OnlineUserVO::getLoginTime, Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    private void fillTokenMeta(OnlineUserVO vo, String token, SaTerminalInfo terminal) {
        // 不自动创建 Token-Session，避免无效 token 抛异常拖垮整次查询
        SaSession tokenSession = null;
        try {
            tokenSession = StpUtil.getStpLogic().getTokenSessionByToken(token, false);
        } catch (Exception ignored) {
            // ignore
        }
        if (tokenSession != null) {
            Object tip = tokenSession.get(TOKEN_LOGIN_IP);
            Object tb = tokenSession.get(TOKEN_BROWSER);
            Object tos = tokenSession.get(TOKEN_OS);
            Object tt = tokenSession.get(TOKEN_LOGIN_TIME);
            vo.setIp(tip == null ? "" : String.valueOf(tip));
            vo.setBrowser(tb == null ? "" : String.valueOf(tb));
            vo.setOs(tos == null ? "" : String.valueOf(tos));
            if (tt instanceof Number n) {
                vo.setLoginTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(n.longValue()), ZoneId.systemDefault()));
                return;
            }
        }
        vo.setIp("");
        vo.setBrowser("");
        vo.setOs(terminal.getDeviceType() == null ? "" : terminal.getDeviceType());
        if (terminal.getCreateTime() > 0) {
            vo.setLoginTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(terminal.getCreateTime()), ZoneId.systemDefault()));
        }
    }

    public void forceLogout(String tokenId) {
        if (tokenId == null || tokenId.isBlank()) {
            throw new BizException("token 不能为空");
        }
        String current = StpUtil.getTokenValue();
        if (tokenId.equals(current)) {
            throw new BizException("不能强退当前登录会话");
        }
        StpUtil.kickoutByTokenValue(tokenId);
    }

    public void forceLogoutBatch(List<String> tokenIds) {
        if (tokenIds == null || tokenIds.isEmpty()) {
            return;
        }
        String current = StpUtil.getTokenValue();
        for (String tokenId : tokenIds) {
            if (tokenId == null || tokenId.isBlank() || tokenId.equals(current)) {
                continue;
            }
            StpUtil.kickoutByTokenValue(tokenId);
        }
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
