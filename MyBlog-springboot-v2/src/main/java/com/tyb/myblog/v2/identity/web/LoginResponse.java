package com.tyb.myblog.v2.identity.web;

import com.tyb.myblog.v2.identity.domain.AuthRole;

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
