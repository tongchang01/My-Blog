package com.tyb.myblog.v2.system.application.friendlink;

import com.tyb.myblog.v2.system.domain.friendlink.FriendLink;

/**
 * 公开友链查询结果，不包含后台状态和审计字段。
 */
public record PublicFriendLinkResult(
        long id,
        String name,
        String url,
        String avatarUrl,
        String description
) {

    public static PublicFriendLinkResult from(FriendLink link) {
        return new PublicFriendLinkResult(
                link.id(),
                link.name(),
                link.url(),
                link.avatarUrl(),
                link.description());
    }
}
