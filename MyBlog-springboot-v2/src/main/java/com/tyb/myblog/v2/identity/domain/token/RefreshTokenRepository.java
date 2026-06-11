package com.tyb.myblog.v2.identity.domain.token;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * refresh token 持久化端口。
 */
public interface RefreshTokenRepository {

    /**
     * 保存 refresh token 摘要及有效期，不持久化明文。
     */
    void save(RefreshTokenRecord token);

    /**
     * 按摘要查询尚未撤销且未过期的记录，并加行锁供轮换或单枚撤销使用。
     */
    Optional<RefreshTokenRecord> findActiveForUpdate(String tokenHash, LocalDateTime now);

    /**
     * 将指定 refresh token 标记为已撤销。
     */
    boolean revoke(long id);

    /**
     * 撤销指定后台用户当前全部有效 refresh token。
     */
    int revokeAllByUserId(long userId);
}
