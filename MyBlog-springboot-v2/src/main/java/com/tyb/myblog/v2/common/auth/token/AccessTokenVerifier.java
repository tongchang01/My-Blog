package com.tyb.myblog.v2.common.auth.token;

import java.util.Optional;

/**
 * 访问令牌验证端口。
 *
 * <p>Security 过滤器只依赖该抽象。后续 identity 可在端口实现中加入
 * {@code token_version} 等用户状态校验，无需让 common 依赖 identity 持久化实现。</p>
 */
public interface AccessTokenVerifier {

    Optional<TokenClaims> verify(String token);
}
