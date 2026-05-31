package com.tyb.myblog.v2.identity.web;

import com.tyb.myblog.v2.identity.domain.AuthRole;

import java.time.Instant;
import java.util.Set;

/**
 * 登录响应。
 *
 * @param accessToken 访问令牌
 * @param expiresAt   访问令牌过期时间
 * @param user        登录用户摘要
 */
public record LoginResponse(
        String accessToken,
        Instant expiresAt,
        User user
) {
    /**
     * 登录用户摘要。
     *
     * @param id       用户 ID
     * @param username 登录用户名
     * @param roles    用户角色
     */
    public record User(String id, String username, Set<AuthRole> roles) {
    }
}
