package com.tyb.myblog.v2.identity.domain.auth;

import java.time.LocalDateTime;
import java.util.OptionalInt;

/**
 * 用户访问令牌版本查询端口。
 */
public interface UserTokenVersionRepository {

    /**
     * 查询未删除后台用户的当前 token 版本。
     *
     * @param userId 后台用户 ID
     * @return 用户不存在或已删除时返回空
     */
    OptionalInt findActiveTokenVersion(long userId);

    /**
     * 查询允许执行 token 刷新的后台用户当前版本。
     *
     * @param userId 后台用户 ID
     * @param now    应用层 Clock 生成的当前时间
     * @return 用户不存在、已删除或仍在锁定期时返回空
     */
    OptionalInt findRefreshableTokenVersion(long userId, LocalDateTime now);

    /**
     * 递增有效用户的 token 版本并记录真实审计信息。
     *
     * @param userId    目标用户 ID
     * @param updatedAt 应用层 Clock 生成的更新时间
     * @param updatedBy 实际操作用户 ID；系统操作时可为空
     * @return 更新到一条有效用户记录时返回 true
     */
    boolean incrementActiveTokenVersion(long userId, LocalDateTime updatedAt, Long updatedBy);
}
