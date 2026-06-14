package com.tyb.myblog.v2.system.web;

import com.tyb.myblog.v2.system.application.friendlink.PublicFriendLinkResult;

/**
 * 公开友链响应。
 */
public record PublicFriendLinkVO(
        long id,
        String name,
        String url,
        String avatarUrl,
        String description
) {

    public static PublicFriendLinkVO from(
            PublicFriendLinkResult result) {
        return new PublicFriendLinkVO(
                result.id(),
                result.name(),
                result.url(),
                result.avatarUrl(),
                result.description());
    }
}
