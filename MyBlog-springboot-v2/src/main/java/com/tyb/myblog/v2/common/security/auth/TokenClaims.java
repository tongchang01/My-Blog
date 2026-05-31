package com.tyb.myblog.v2.common.security.auth;

import java.time.Instant;
import java.util.List;

/**
 * 已解析的 JWT 声明。
 *
 * <p>该对象是认证过滤器和业务认证适配器之间使用的安全载荷，
 * 不直接暴露给前端接口。</p>
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
