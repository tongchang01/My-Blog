package com.tyb.myblog.v2.identity.application.token;

import com.tyb.myblog.v2.common.auth.token.AccessTokenDecoder;
import com.tyb.myblog.v2.common.auth.token.AccessTokenVerifier;
import com.tyb.myblog.v2.common.auth.token.TokenClaims;
import com.tyb.myblog.v2.identity.domain.auth.UserTokenVersionRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * 结合 JWT 声明与用户持久化状态验证访问令牌。
 */
@Service
public class PersistentAccessTokenVerifier implements AccessTokenVerifier {

    private final AccessTokenDecoder tokenDecoder;
    private final UserTokenVersionRepository userTokenVersionRepository;

    public PersistentAccessTokenVerifier(
            AccessTokenDecoder tokenDecoder,
            UserTokenVersionRepository userTokenVersionRepository) {
        this.tokenDecoder = tokenDecoder;
        this.userTokenVersionRepository = userTokenVersionRepository;
    }

    /**
     * 校验 JWT 本身及后台用户当前持久化状态。
     *
     * <p>只有用户仍存在、未删除且 token_version 与 JWT 声明一致时才接受访问令牌。
     * 该校验保证整体撤销后，旧 access token 立即失效。</p>
     *
     * @param token access token 明文
     * @return 校验成功时返回令牌声明，否则返回空
     */
    @Override
    public Optional<TokenClaims> verify(String token) {
        return tokenDecoder.decode(token).flatMap(this::verifyUserState);
    }

    private Optional<TokenClaims> verifyUserState(TokenClaims claims) {
        try {
            long userId = Long.parseLong(claims.userId());
            OptionalInt currentVersion = userTokenVersionRepository.findActiveTokenVersion(userId);
            if (currentVersion.isEmpty() || currentVersion.getAsInt() != claims.tokenVersion()) {
                return Optional.empty();
            }
            return Optional.of(claims);
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }
}
