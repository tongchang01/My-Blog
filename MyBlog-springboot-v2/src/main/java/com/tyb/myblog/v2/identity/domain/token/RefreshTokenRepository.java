package com.tyb.myblog.v2.identity.domain.token;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * refresh token 持久化端口。
 */
public interface RefreshTokenRepository {

    void save(RefreshTokenRecord token);

    Optional<RefreshTokenRecord> findActiveForUpdate(String tokenHash, LocalDateTime now);

    boolean revoke(long id);

    int revokeAllByUserId(long userId);
}
