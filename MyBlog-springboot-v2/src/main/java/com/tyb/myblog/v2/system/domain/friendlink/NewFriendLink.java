package com.tyb.myblog.v2.system.domain.friendlink;

/**
 * 待登记的新友链。
 */
public record NewFriendLink(
        String name,
        String url,
        String avatarUrl,
        String description,
        int sortOrder,
        FriendLinkStatus status,
        long createdBy
) {

    public static NewFriendLink create(
            String name,
            String url,
            String avatarUrl,
            String description,
            int sortOrder,
            FriendLinkStatus status,
            long createdBy) {
        if (createdBy <= 0) {
            throw new IllegalArgumentException(
                    "友链创建人 ID 必须为正数");
        }
        FriendLinkValidation.Values values =
                FriendLinkValidation.validate(
                        name,
                        url,
                        avatarUrl,
                        description,
                        sortOrder,
                        status);
        return new NewFriendLink(
                values.name(),
                values.url(),
                values.avatarUrl(),
                values.description(),
                values.sortOrder(),
                values.status(),
                createdBy);
    }
}
