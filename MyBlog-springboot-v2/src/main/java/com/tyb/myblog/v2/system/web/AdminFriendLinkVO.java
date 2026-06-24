package com.tyb.myblog.v2.system.web;

import com.tyb.myblog.v2.system.application.friendlink.FriendLinkResult;
import com.tyb.myblog.v2.system.domain.friendlink.FriendLinkStatus;

import java.time.LocalDateTime;

/**
 * 后台友链响应。
 */
public record AdminFriendLinkVO(
        String id,
        String name,
        String url,
        String avatarUrl,
        String description,
        int sortOrder,
        FriendLinkStatus status,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy
) {

    public static AdminFriendLinkVO from(FriendLinkResult result) {
        return new AdminFriendLinkVO(
                Long.toString(result.id()),
                result.name(),
                result.url(),
                result.avatarUrl(),
                result.description(),
                result.sortOrder(),
                result.status(),
                result.createdAt(),
                result.createdBy() == null
                        ? null
                        : Long.toString(result.createdBy()),
                result.updatedAt(),
                result.updatedBy() == null
                        ? null
                        : Long.toString(result.updatedBy()));
    }
}
