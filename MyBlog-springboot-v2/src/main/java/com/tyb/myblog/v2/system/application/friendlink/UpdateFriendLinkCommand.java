package com.tyb.myblog.v2.system.application.friendlink;

import com.tyb.myblog.v2.system.domain.friendlink.FriendLinkStatus;

/**
 * 完整编辑友链命令。
 */
public record UpdateFriendLinkCommand(
        String name,
        String url,
        String avatarUrl,
        String description,
        int sortOrder,
        FriendLinkStatus status
) {
}
