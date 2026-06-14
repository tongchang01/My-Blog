package com.tyb.myblog.v2.system.web;

import com.tyb.myblog.v2.system.application.friendlink.UpdateFriendLinkCommand;

/**
 * 完整编辑友链 HTTP 请求。
 */
public class UpdateFriendLinkRequest
        extends FriendLinkWriteRequestSupport {

    public UpdateFriendLinkCommand toCommand() {
        Values values = values();
        return new UpdateFriendLinkCommand(
                values.name(),
                values.url(),
                values.avatarUrl(),
                values.description(),
                values.sortOrder(),
                values.status());
    }
}
