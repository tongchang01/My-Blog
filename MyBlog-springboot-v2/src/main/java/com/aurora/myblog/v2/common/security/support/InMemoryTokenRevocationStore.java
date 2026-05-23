package com.aurora.myblog.v2.common.security.support;

import com.aurora.myblog.v2.common.security.auth.TokenRevocationStore;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryTokenRevocationStore implements TokenRevocationStore {

    private final Map<String, Instant> revokedTokenIds = new ConcurrentHashMap<>();

    @Override
    public void revoke(String tokenId, Instant expiresAt) {
        revokedTokenIds.put(tokenId, expiresAt);
    }

    @Override
    public boolean isRevoked(String tokenId) {
        Instant expiresAt = revokedTokenIds.get(tokenId);
        if (expiresAt == null) {
            return false;
        }
        if (expiresAt.isBefore(Instant.now())) {
            revokedTokenIds.remove(tokenId);
            return false;
        }
        return true;
    }
}
