package com.tyb.myblog.v2.infrastructure.security;

import com.tyb.myblog.v2.common.security.auth.JwtTokenService;
import com.tyb.myblog.v2.identity.application.AuthTokenService;
import com.tyb.myblog.v2.identity.domain.AuthRole;
import com.tyb.myblog.v2.identity.domain.AuthenticatedUser;
import org.springframework.stereotype.Component;

/**
 * identity 模块认证令牌端口的 JWT 适配器。
 *
 * <p>identity application 只依赖 {@link AuthTokenService} 端口，不直接依赖 JWT 实现。
 * 该适配器把已认证用户转换为通用 JWT 服务需要的签发参数。</p>
 */
@Component
public class JwtAuthTokenServiceAdapter implements AuthTokenService {

    /**
     * 通用 JWT 访问令牌服务。
     */
    private final JwtTokenService tokenService;

    /**
     * 创建 JWT 认证令牌适配器。
     *
     * @param tokenService 通用 JWT 服务
     */
    public JwtAuthTokenServiceAdapter(JwtTokenService tokenService) {
        this.tokenService = tokenService;
    }

    /**
     * 为登录成功的用户签发访问令牌。
     *
     * @param user 已认证用户
     * @return identity 模块使用的令牌签发结果
     */
    @Override
    public TokenIssueResult issueAccessToken(AuthenticatedUser user) {
        var token = tokenService.issueAccessToken(
                user.id(),
                user.username(),
                user.roles().stream().map(AuthRole::name).toList());
        return new TokenIssueResult(token.accessToken(), token.expiresAt());
    }

    /**
     * 撤销访问令牌。
     *
     * @param accessToken 原始访问令牌
     */
    @Override
    public void revoke(String accessToken) {
        tokenService.revoke(accessToken);
    }
}
