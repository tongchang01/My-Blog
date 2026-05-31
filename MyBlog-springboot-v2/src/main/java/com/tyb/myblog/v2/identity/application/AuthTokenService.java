package com.tyb.myblog.v2.identity.application;

import com.tyb.myblog.v2.identity.domain.AuthenticatedUser;

import java.time.Instant;

public interface AuthTokenService {
    TokenIssueResult issueAccessToken(AuthenticatedUser user);

    void revoke(String accessToken);

    record TokenIssueResult(String accessToken, Instant expiresAt) {
    }
}
