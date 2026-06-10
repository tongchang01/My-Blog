package com.tyb.myblog.v2.identity.application.token;

import java.time.LocalDateTime;

/**
 * 仅在签发时返回给调用方的 refresh token 明文。
 */
public record IssuedRefreshToken(
        long userId,
        String token,
        LocalDateTime expiresAt
) {
}
