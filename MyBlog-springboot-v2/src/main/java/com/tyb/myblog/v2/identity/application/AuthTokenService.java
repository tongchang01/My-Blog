package com.tyb.myblog.v2.identity.application;

import com.tyb.myblog.v2.identity.domain.AuthenticatedUser;

import java.time.Instant;

/**
 * 认证令牌服务端口。
 *
 * <p>identity 模块通过该端口签发和撤销访问令牌，不绑定具体 JWT 实现。
 * 当前实现由全局 infrastructure 中的 JWT 适配器提供。</p>
 */
public interface AuthTokenService {
    /**
     * 为已认证用户签发访问令牌。
     */
    TokenIssueResult issueAccessToken(AuthenticatedUser user);

    /**
     * 撤销访问令牌，用于登出场景。
     */
    void revoke(String accessToken);

    /**
     * 令牌签发结果。
     *
     * @param accessToken 访问令牌
     * @param expiresAt   过期时间
     */
    record TokenIssueResult(String accessToken, Instant expiresAt) {
    }
}
