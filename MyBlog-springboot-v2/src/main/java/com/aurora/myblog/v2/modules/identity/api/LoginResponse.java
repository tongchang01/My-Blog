package com.aurora.myblog.v2.modules.identity.api;

import com.aurora.myblog.v2.modules.identity.domain.AuthRole;

import java.time.Instant;
import java.util.Set;

public record LoginResponse(
        String accessToken,
        Instant expiresAt,
        User user
) {
    public record User(String id, String username, Set<AuthRole> roles) {
    }
}
