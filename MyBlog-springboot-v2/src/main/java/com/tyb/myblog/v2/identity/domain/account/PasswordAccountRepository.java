package com.tyb.myblog.v2.identity.domain.account;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 修改密码账号持久化端口。
 */
public interface PasswordAccountRepository {

    /**
     * 锁定并读取未删除账号的改密快照。
     *
     * @param userId 账号 ID
     * @return 可修改密码的账号快照
     */
    Optional<ChangeablePasswordAccount> findActiveByIdForUpdate(long userId);

    /**
     * 原子更新密码摘要并递增 token version。
     *
     * @param userId 账号 ID
     * @param passwordHash 新密码摘要
     * @param updatedAt 更新时间
     * @param updatedBy 操作人 ID
     * @return 恰好更新一行时返回 true
     */
    boolean updatePasswordAndIncrementTokenVersion(
            long userId,
            String passwordHash,
            LocalDateTime updatedAt,
            Long updatedBy);
}
