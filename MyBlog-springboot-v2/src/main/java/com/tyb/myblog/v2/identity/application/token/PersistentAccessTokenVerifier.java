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
