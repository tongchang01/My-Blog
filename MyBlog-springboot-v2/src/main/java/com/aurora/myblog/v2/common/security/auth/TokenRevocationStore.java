package com.aurora.myblog.v2.common.security.auth;

import java.time.Instant;

public interface TokenRevocationStore {
    void revoke(String tokenId, Instant expiresAt);

    boolean isRevoked(String tokenId);
}
