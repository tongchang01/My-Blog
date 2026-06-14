package com.tyb.myblog.v2.system.application.friendlink;

import com.tyb.myblog.v2.system.domain.friendlink.FriendLink;
import com.tyb.myblog.v2.system.domain.friendlink.FriendLinkStatus;

import java.time.LocalDateTime;

/**
 * 后台友链查询结果。
 */
public record FriendLinkResult(
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

    public static FriendLinkResult from(FriendLink link) {
        return new FriendLinkResult(
                link.id(),
                link.name(),
                link.url(),
                link.avatarUrl(),
                link.description(),
                link.sortOrder(),
                link.status(),
                link.createdAt(),
                link.createdBy(),
                link.updatedAt(),
                link.updatedBy());
    }
}
