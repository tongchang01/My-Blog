package com.aurora.myblog.v2.modules.identity.application;

import com.aurora.myblog.v2.modules.identity.domain.AuthenticatedUser;

import java.time.Instant;

public interface AuthTokenService {
    TokenIssueResult issueAccessToken(AuthenticatedUser user);

    void revoke(String accessToken);

    record TokenIssueResult(String accessToken, Instant expiresAt) {
    }
}
