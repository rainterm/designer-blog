package com.designer.admin.system.service;

import cn.dev33.satoken.secure.BCrypt;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.framework.satoken.StpOAuthUtil;
import com.designer.admin.system.model.entity.SysOauthClient;
import com.designer.admin.system.model.vo.Oauth2TokenVO;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * OAuth2 Client Credentials 换票
 */
@Component
public class Oauth2TokenService {

    @Inject
    private SysOauthClientService clientService;

    public Oauth2TokenVO issueClientCredentials(String grantType, String clientId, String clientSecret, String scope) {
        if (grantType == null || !"client_credentials".equalsIgnoreCase(grantType.trim())) {
            throw new BizException("unsupported_grant_type");
        }
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw new BizException("invalid_client");
        }
        SysOauthClient client = clientService.getEnabledByClientId(clientId.trim());
        if (client == null || !BCrypt.checkpw(clientSecret, client.getClientSecret())) {
            throw new BizException("invalid_client");
        }
        List<String> allowed = clientService.listAuthorizedScopeCodes(client.getId());
        if (allowed == null) {
            allowed = List.of();
        }
        List<String> granted = resolveGrantedScopes(scope, allowed);
        if (granted.isEmpty()) {
            throw new BizException("invalid_scope");
        }
        int ttl = client.getAccessTokenTtl() == null ? 7200 : client.getAccessTokenTtl();
        StpOAuthUtil.login(client.getClientId(), granted, ttl);

        Oauth2TokenVO vo = new Oauth2TokenVO();
        vo.setAccess_token(StpOAuthUtil.getTokenValue());
        vo.setToken_type("Bearer");
        vo.setExpires_in((long) ttl);
        vo.setScope(String.join(" ", granted));
        return vo;
    }

    private static List<String> resolveGrantedScopes(String requested, List<String> allowed) {
        Set<String> allowSet = allowed.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (requested == null || requested.isBlank()) {
            return new ArrayList<>(allowSet);
        }
        List<String> req = Arrays.stream(requested.trim().split("\\s+"))
                .filter(s -> !s.isBlank())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
        List<String> granted = new ArrayList<>();
        for (String s : req) {
            if (!allowSet.contains(s)) {
                throw new BizException("invalid_scope");
            }
            granted.add(s);
        }
        return granted;
    }
}
