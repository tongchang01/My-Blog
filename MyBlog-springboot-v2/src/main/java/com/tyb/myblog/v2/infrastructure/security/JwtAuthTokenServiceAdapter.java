package com.tyb.myblog.v2.infrastructure.security;

import com.tyb.myblog.v2.common.security.auth.JwtTokenService;
import com.tyb.myblog.v2.identity.application.AuthTokenService;
import com.tyb.myblog.v2.identity.domain.AuthRole;
import com.tyb.myblog.v2.identity.domain.AuthenticatedUser;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthTokenServiceAdapter implements AuthTokenService {

    private final JwtTokenService tokenService;

    public JwtAuthTokenServiceAdapter(JwtTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public TokenIssueResult issueAccessToken(AuthenticatedUser user) {
        var token = tokenService.issueAccessToken(
                user.id(),
                user.username(),
                user.roles().stream().map(AuthRole::name).toList());
        return new TokenIssueResult(token.accessToken(), token.expiresAt());
    }

    @Override
    public void revoke(String accessToken) {
        tokenService.revoke(accessToken);
    }
}
