package com.tyb.myblog.v2.system.application.friendlink;

import com.tyb.myblog.v2.system.domain.friendlink.FriendLinkStatus;

/**
 * 新增友链命令。
 */
public record CreateFriendLinkCommand(
        String name,
        String url,
        String avatarUrl,
        String description,
        int sortOrder,
        FriendLinkStatus status
) {
}
