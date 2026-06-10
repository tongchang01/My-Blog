package com.tyb.myblog.v2.common.auth.token;

import java.time.Instant;
import java.util.List;

/**
 * 已验证的访问令牌声明。
 *
 * @param tokenId   token 唯一标识，对应 JWT {@code jti}
 * @param userId    当前登录用户 ID，对应 JWT {@code sub}
 * @param username  登录用户名
 * @param roles     用户角色名称列表，不包含 {@code ROLE_} 前缀
 * @param expiresAt token 过期时间
 */
public record TokenClaims(
        String tokenId,
        String userId,
        String username,
        List<String> roles,
        Instant expiresAt
) {
}
