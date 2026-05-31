package com.tyb.myblog.v2.common.security.support;

import com.tyb.myblog.v2.common.security.auth.TokenRevocationStore;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于内存的 token 撤销存储。
 *
 * <p>用于当前单体本地运行和早期 V2 验证。该实现不适合多实例部署，
 * 因为撤销状态不会在不同服务进程之间共享；后续需要按部署形态替换为 Redis 等共享存储。</p>
 */
@Component
public class InMemoryTokenRevocationStore implements TokenRevocationStore {

    /**
     * 已撤销 token 的过期时间索引。
     *
     * <p>key 为 JWT {@code jti}，value 为 token 原始过期时间。</p>
     */
    private final Map<String, Instant> revokedTokenIds = new ConcurrentHashMap<>();

    /**
     * 记录已撤销 token。
     */
    @Override
    public void revoke(String tokenId, Instant expiresAt) {
        revokedTokenIds.put(tokenId, expiresAt);
    }

    /**
     * 判断 token 是否仍处于撤销有效期内。
     */
    @Override
    public boolean isRevoked(String tokenId) {
        Instant expiresAt = revokedTokenIds.get(tokenId);
        if (expiresAt == null) {
            return false;
        }
        if (expiresAt.isBefore(Instant.now())) {
            // token 已自然过期后，撤销记录不再有安全价值，可以懒清理。
            revokedTokenIds.remove(tokenId);
            return false;
        }
        return true;
    }
}
