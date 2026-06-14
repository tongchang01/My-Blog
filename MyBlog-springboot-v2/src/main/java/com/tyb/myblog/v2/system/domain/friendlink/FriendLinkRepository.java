package com.tyb.myblog.v2.system.domain.friendlink;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 友链仓储端口。
 */
public interface FriendLinkRepository {

    List<FriendLink> findPublicVisible();

    FriendLinkPage findActivePage(int page, int size);

    Optional<FriendLink> findActiveById(long id);

    Optional<FriendLink> findActiveByIdForUpdate(long id);

    List<FriendLink> findActiveByIdsForUpdate(List<Long> ids);

    FriendLink insert(NewFriendLink friendLink);

    boolean update(
            FriendLink friendLink,
            LocalDateTime updatedAt,
            long updatedBy);

    boolean updateStatus(
            long id,
            FriendLinkStatus status,
            LocalDateTime updatedAt,
            long updatedBy);

    boolean updateSortOrder(
            long id,
            int sortOrder,
            LocalDateTime updatedAt,
            long updatedBy);

    boolean softDelete(
            long id,
            LocalDateTime deletedAt,
            long deletedBy);
}
