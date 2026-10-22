package com.designer.admin.system.service.auth.impl;

import com.designer.admin.common.exception.BizException;
import com.designer.admin.framework.crypto.PasswordUtil;
import com.designer.admin.framework.satoken.StpOAuthUtil;
import com.designer.admin.system.constant.Oauth2Consts;
import com.designer.admin.system.model.entity.auth.SysOauthClient;
import com.designer.admin.system.model.vo.auth.Oauth2TokenVO;
import com.designer.admin.system.service.auth.Oauth2TokenService;
import com.designer.admin.system.service.auth.SysOauthClientService;
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
 *
 * @author 雨夜终端
 */
@Component
public class Oauth2TokenServiceImpl implements Oauth2TokenService {

    @Inject
    private SysOauthClientService clientService;

    @Override
    public Oauth2TokenVO issueClientCredentials(String clientId, String clientSecret, String scope) {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw new BizException(Oauth2Consts.ERROR_INVALID_CLIENT);
        }
        SysOauthClient client = clientService.getEnabledByClientId(clientId.trim());
        if (client == null || !PasswordUtil.matches(clientSecret, client.getClientSecret())) {
            throw new BizException(Oauth2Consts.ERROR_INVALID_CLIENT);
        }
        List<String> allowed = clientService.listAuthorizedScopeCodes(client.getId());
        if (allowed == null) {
            allowed = List.of();
        }
        List<String> granted = resolveGrantedScopes(scope, allowed);
        if (granted.isEmpty()) {
            throw new BizException(Oauth2Consts.ERROR_INVALID_SCOPE);
        }
        int ttl = client.getAccessTokenTtl() == null
                ? Oauth2Consts.DEFAULT_ACCESS_TOKEN_TTL_SECONDS : client.getAccessTokenTtl();
        StpOAuthUtil.login(client.getClientId(), granted, ttl);

        return Oauth2TokenVO.builder()
                .accessToken(StpOAuthUtil.getTokenValue())
                .tokenType(Oauth2Consts.TOKEN_TYPE_BEARER)
                .expiresIn((long) ttl)
                .scope(String.join(" ", granted))
                .build();
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
                throw new BizException(Oauth2Consts.ERROR_INVALID_SCOPE);
            }
            granted.add(s);
        }
        return granted;
    }
}
