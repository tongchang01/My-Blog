package com.tyb.myblog.v2.common.security.auth;

import java.time.Instant;

/**
 * 签发后的访问令牌。
 *
 * @param accessToken JWT 字符串
 * @param expiresAt   token 过期时间
 */
public record TokenPair(String accessToken, Instant expiresAt) {
}
