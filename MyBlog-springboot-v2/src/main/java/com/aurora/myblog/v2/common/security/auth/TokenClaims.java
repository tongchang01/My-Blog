package com.aurora.myblog.v2.common.security.auth;

import java.time.Instant;
import java.util.List;

public record TokenClaims(
        String tokenId,
        String userId,
        String username,
        List<String> roles,
        Instant expiresAt
) {
}
