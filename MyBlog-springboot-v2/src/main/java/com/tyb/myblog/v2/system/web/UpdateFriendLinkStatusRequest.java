package com.tyb.myblog.v2.system.web;

import com.tyb.myblog.v2.system.application.friendlink.UpdateFriendLinkStatusCommand;
import com.tyb.myblog.v2.system.domain.friendlink.FriendLinkStatus;

/**
 * 友链状态修改请求。
 */
public record UpdateFriendLinkStatusRequest(
        FriendLinkStatus status
) {

    public UpdateFriendLinkStatusCommand toCommand() {
        return new UpdateFriendLinkStatusCommand(status);
    }
}
