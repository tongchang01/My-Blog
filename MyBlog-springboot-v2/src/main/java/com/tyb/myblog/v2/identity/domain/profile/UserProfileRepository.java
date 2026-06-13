package com.tyb.myblog.v2.identity.domain.profile;

import java.util.Optional;

/**
 * 用户资料仓储端口，隔离领域模型与具体数据库实现。
 */
public interface UserProfileRepository {

    /**
     * 按用户 ID 查询未删除资料。
     *
     * @param userId 用户 ID
     * @return 未删除的用户资料，不存在时返回空
     */
    Optional<UserProfile> findActiveByUserId(long userId);

    /**
     * 按用户 ID 加锁查询未删除资料。
     *
     * @param userId 用户 ID
     * @return 未删除的用户资料，不存在时返回空
     */
    Optional<UserProfile> findActiveByUserIdForUpdate(long userId);

    /**
     * 创建用户资料。
     *
     * @param profile 已完成领域校验的用户资料
     */
    void insert(UserProfile profile);

    /**
     * 更新未删除的用户资料。
     *
     * @param profile 已完成领域校验的用户资料
     * @return 恰好更新一行时返回 {@code true}
     */
    boolean update(UserProfile profile);
}
