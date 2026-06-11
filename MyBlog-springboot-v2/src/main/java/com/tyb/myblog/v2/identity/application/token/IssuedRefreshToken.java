package com.tyb.myblog.v2.identity.application.token;

import java.time.LocalDateTime;

/**
 * 仅在签发时返回给调用方的 refresh token 明文。
 */
public record IssuedRefreshToken(
        /** token 所属的后台用户 ID。 */
        long userId,
        /** 仅在签发或轮换成功时返回的 token 明文。 */
        String token,
        /** 按应用统一时区计算的过期时间。 */
        LocalDateTime expiresAt
) {
}
