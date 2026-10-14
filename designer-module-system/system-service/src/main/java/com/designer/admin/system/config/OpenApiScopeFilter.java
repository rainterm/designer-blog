package com.designer.admin.system.config;

import cn.dev33.satoken.exception.NotLoginException;
import com.designer.admin.common.core.R;
import com.designer.admin.common.core.ResultCode;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.common.util.JsonUtil;
import com.designer.admin.framework.satoken.StpOAuthUtil;
import com.designer.admin.system.service.OpenApiScopeGuard;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.Filter;
import org.noear.solon.core.handle.FilterChain;

/**
 * /open-api/**：OAuth2 access_token 登录校验 + Scope 路径授权。
 */
@Component(index = -50)
public class OpenApiScopeFilter implements Filter {

    @Inject
    private OpenApiScopeGuard scopeGuard;

    @Override
    public void doFilter(Context ctx, FilterChain chain) throws Throwable {
        String path = ctx.path();
        if (path != null && path.startsWith("/open-api/")) {
            try {
                // Postman 等会带 Bearer 前缀；写入 Storage 后再校验，避免读头失败
                StpOAuthUtil.checkLoginByAuthorization(ctx.header("Authorization"));
                scopeGuard.checkCurrentRequest();
            } catch (NotLoginException e) {
                write(ctx, R.fail(ResultCode.UNAUTHORIZED, "未授权或 access_token 无效"));
                return;
            } catch (BizException e) {
                write(ctx, R.fail(e.getCode(), e.getMessage()));
                return;
            }
        }
        chain.doFilter(ctx);
    }

    private void write(Context ctx, R<?> body) {
        ctx.setRendered(false);
        ctx.contentType("application/json;charset=utf-8");
        ctx.output(JsonUtil.toJson(body));
        ctx.setRendered(true);
    }
}
