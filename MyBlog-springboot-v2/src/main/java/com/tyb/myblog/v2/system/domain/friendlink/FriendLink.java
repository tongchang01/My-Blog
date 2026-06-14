package com.tyb.myblog.v2.system.domain.friendlink;

import java.time.LocalDateTime;

/**
 * 有效友链聚合。
 */
public record FriendLink(
        long id,
        String name,
        String url,
        String avatarUrl,
        String description,
        int sortOrder,
        FriendLinkStatus status,
        LocalDateTime createdAt,
        Long createdBy,
        LocalDateTime updatedAt,
        Long updatedBy
) {

    public static FriendLink reconstitute(
            long id,
            String name,
            String url,
            String avatarUrl,
            String description,
            int sortOrder,
            FriendLinkStatus status,
            LocalDateTime createdAt,
            Long createdBy,
            LocalDateTime updatedAt,
            Long updatedBy) {
        if (id <= 0) {
            throw new IllegalArgumentException(
                    "友链 ID 必须为正数");
        }
        FriendLinkValidation.Values values =
                FriendLinkValidation.validate(
                        name,
                        url,
                        avatarUrl,
                        description,
                        sortOrder,
                        status);
        return new FriendLink(
                id,
                values.name(),
                values.url(),
                values.avatarUrl(),
                values.description(),
                values.sortOrder(),
                values.status(),
                createdAt,
                createdBy,
                updatedAt,
                updatedBy);
    }

    public FriendLink replace(
            String name,
            String url,
            String avatarUrl,
            String description,
            int sortOrder,
            FriendLinkStatus status) {
        return reconstitute(
                id,
                name,
                url,
                avatarUrl,
                description,
                sortOrder,
                status,
                createdAt,
                createdBy,
                updatedAt,
                updatedBy);
    }

    public FriendLink withStatus(FriendLinkStatus newStatus) {
        return replace(
                name,
                url,
                avatarUrl,
                description,
                sortOrder,
                newStatus);
    }
}
