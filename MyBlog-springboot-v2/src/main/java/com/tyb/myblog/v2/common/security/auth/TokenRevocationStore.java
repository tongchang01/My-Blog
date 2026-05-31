package com.tyb.myblog.v2.common.security.auth;

import java.time.Instant;

/**
 * token 撤销存储端口。
 *
 * <p>用于登出后让尚未过期的 JWT 失效。当前 V2 先使用内存实现，
 * 后续如果多实例部署，需要替换为 Redis 等共享存储。</p>
 */
public interface TokenRevocationStore {
    /**
     * 撤销指定 token。
     *
     * @param tokenId   token 唯一标识
     * @param expiresAt token 原始过期时间
     */
    void revoke(String tokenId, Instant expiresAt);

    /**
     * 判断 token 是否已撤销。
     *
     * @param tokenId token 唯一标识
     * @return 已撤销返回 {@code true}
     */
    boolean isRevoked(String tokenId);
}
