package com.tyb.myblog.v2.system.web;

import com.tyb.myblog.v2.system.application.friendlink.CreateFriendLinkCommand;

/**
 * 新增友链 HTTP 请求。
 */
public class CreateFriendLinkRequest
        extends FriendLinkWriteRequestSupport {

    public CreateFriendLinkCommand toCommand() {
        Values values = values();
        return new CreateFriendLinkCommand(
                values.name(),
                values.url(),
                values.avatarUrl(),
                values.description(),
                values.sortOrder(),
                values.status());
    }
}
