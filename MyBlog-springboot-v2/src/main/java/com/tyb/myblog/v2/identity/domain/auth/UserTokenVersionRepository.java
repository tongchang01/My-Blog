package com.tyb.myblog.v2.identity.domain.auth;

import java.time.LocalDateTime;
import java.util.OptionalInt;

/**
 * 用户访问令牌版本查询端口。
 */
public interface UserTokenVersionRepository {

    OptionalInt findActiveTokenVersion(long userId);

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
