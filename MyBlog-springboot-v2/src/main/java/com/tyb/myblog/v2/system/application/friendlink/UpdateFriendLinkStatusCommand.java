package com.tyb.myblog.v2.system.application.friendlink;

import com.tyb.myblog.v2.system.domain.friendlink.FriendLinkStatus;

/**
 * 修改友链展示状态命令。
 */
public record UpdateFriendLinkStatusCommand(
        FriendLinkStatus status
) {
}
