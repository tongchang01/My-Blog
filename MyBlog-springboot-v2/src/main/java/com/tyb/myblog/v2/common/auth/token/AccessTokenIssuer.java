package com.tyb.myblog.v2.common.auth.token;

import java.util.List;

/**
 * 访问令牌签发端口。
 *
 * <p>identity 应用层通过该抽象签发令牌，不依赖 JWT 或 Spring Security 具体实现。</p>
 */
public interface AccessTokenIssuer {

    TokenPair issueAccessToken(String userId, String username, List<String> roles, int tokenVersion);
}
