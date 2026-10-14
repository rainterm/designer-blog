package com.designer.admin.framework.satoken;

import cn.dev33.satoken.stp.StpUtil;

/**
 * 登录用户读取辅助
 */
public final class LoginHelper {

    public static final String LOGIN_USER_KEY = "loginUser";

    private LoginHelper() {
    }

    public static Long getUserId() {
        return Long.valueOf(StpUtil.getLoginId().toString());
    }

    public static String getUsername() {
        Object obj = StpUtil.getSession().get(LOGIN_USER_KEY);
        return obj instanceof LoginUser lu ? lu.getUsername() : "";
    }
}

