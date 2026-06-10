package com.tyb.myblog.v2.identity.domain.token;

import java.time.LocalDateTime;

/**
 * refresh token 的持久化领域记录。
 */
public record RefreshTokenRecord(
        Long id,
        long userId,
        String tokenHash,
        LocalDateTime expiresAt,
        boolean revoked
) {
    public static RefreshTokenRecord active(long userId, String tokenHash, LocalDateTime expiresAt) {
        return new RefreshTokenRecord(null, userId, tokenHash, expiresAt, false);
    }
}
