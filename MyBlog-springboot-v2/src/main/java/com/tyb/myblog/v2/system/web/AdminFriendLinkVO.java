package com.tyb.myblog.v2.system.web;

import com.tyb.myblog.v2.system.application.friendlink.FriendLinkResult;
import com.tyb.myblog.v2.system.domain.friendlink.FriendLinkStatus;

import java.time.LocalDateTime;

/**
 * 后台友链响应。
 */
public record AdminFriendLinkVO(
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

    public static AdminFriendLinkVO from(FriendLinkResult result) {
        return new AdminFriendLinkVO(
                result.id(),
                result.name(),
                result.url(),
                result.avatarUrl(),
                result.description(),
                result.sortOrder(),
                result.status(),
                result.createdAt(),
                result.createdBy(),
                result.updatedAt(),
                result.updatedBy());
    }
}
