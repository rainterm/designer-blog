package com.designer.admin.system.controller.open;

import cn.dev33.satoken.annotation.SaIgnore;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.common.util.JsonUtil;
import com.designer.admin.system.model.vo.Oauth2TokenVO;
import com.designer.admin.system.service.Oauth2TokenService;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Post;
import org.noear.solon.core.handle.Context;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * OAuth2 令牌端点（Client Credentials）。
 * 成功时直接返回 RFC6749 JSON，不包 R 外壳。
 */
@Controller
@Mapping("/oauth2")
public class Oauth2TokenController {

    @Inject
    private Oauth2TokenService tokenService;

    @SaIgnore
    @Post
    @Mapping("/token")
    public void token(Context ctx) {
        String grantType = firstNonBlank(ctx.param("grant_type"), ctx.param("grantType"));
        String clientId = firstNonBlank(ctx.param("client_id"), ctx.param("clientId"));
        String clientSecret = firstNonBlank(ctx.param("client_secret"), ctx.param("clientSecret"));
        String scope = firstNonBlank(ctx.param("scope"), null);
        // 兼容 Basic Auth: Authorization: Basic base64(id:secret)
        String auth = ctx.header("Authorization");
        if ((clientId == null || clientSecret == null) && auth != null && auth.regionMatches(true, 0, "Basic ", 0, 6)) {
            try {
                String decoded = new String(java.util.Base64.getDecoder().decode(auth.substring(6).trim()));
                int idx = decoded.indexOf(':');
                if (idx > 0) {
                    if (clientId == null) {
                        clientId = decoded.substring(0, idx);
                    }
                    if (clientSecret == null) {
                        clientSecret = decoded.substring(idx + 1);
                    }
                }
            } catch (Exception ignored) {
                // fall through
            }
        }
        try {
            Oauth2TokenVO vo = tokenService.issueClientCredentials(grantType, clientId, clientSecret, scope);
            writeJson(ctx, 200, vo);
        } catch (BizException e) {
            Map<String, Object> err = new LinkedHashMap<>();
            String msg = e.getMessage() == null ? "invalid_request" : e.getMessage();
            String error = switch (msg) {
                case "unsupported_grant_type", "invalid_client", "invalid_scope" -> msg;
                default -> "invalid_request";
            };
            err.put("error", error);
            err.put("error_description", msg);
            writeJson(ctx, 400, err);
        }
    }

    private static void writeJson(Context ctx, int status, Object body) {
        ctx.status(status);
        ctx.contentType("application/json;charset=utf-8");
        ctx.output(JsonUtil.toJson(body));
        ctx.setRendered(true);
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        if (b != null && !b.isBlank()) {
            return b;
        }
        return null;
    }
}
