package com.aurora.myblog.v2.common.security.auth;

import java.time.Instant;

public record TokenPair(String accessToken, Instant expiresAt) {
}
