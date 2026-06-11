package com.tyb.myblog.v2.identity.domain.token;

import java.time.LocalDateTime;

/**
 * refresh token 的持久化领域记录。
 */
public record RefreshTokenRecord(
        /** 持久化记录 ID；尚未保存时为空。 */
        Long id,
        /** token 所属的后台用户 ID。 */
        long userId,
        /** refresh token 明文的 SHA-256 摘要。 */
        String tokenHash,
        /** 按应用统一时区记录的过期时间。 */
        LocalDateTime expiresAt,
        /** 是否已被撤销。 */
        boolean revoked
) {
    /**
     * 创建一条尚未持久化的有效 refresh token 记录。
     */
    public static RefreshTokenRecord active(long userId, String tokenHash, LocalDateTime expiresAt) {
        return new RefreshTokenRecord(null, userId, tokenHash, expiresAt, false);
    }
}
