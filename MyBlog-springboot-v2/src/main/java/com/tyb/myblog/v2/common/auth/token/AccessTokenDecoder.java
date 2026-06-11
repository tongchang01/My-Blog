package com.tyb.myblog.v2.common.auth.token;

import java.util.Optional;

/**
 * 访问令牌声明解码端口。
 *
 * <p>只校验令牌签名、标准声明和访问令牌类型，不读取用户持久化状态。</p>
 */
public interface AccessTokenDecoder {

    Optional<TokenClaims> decode(String token);
}
