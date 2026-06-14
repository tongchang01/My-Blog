package com.tyb.myblog.v2.system.domain.friendlink;

import java.util.List;

/**
 * 友链领域分页结果。
 */
public record FriendLinkPage(
        List<FriendLink> records,
        long total,
        int page,
        int size
) {

    public FriendLinkPage {
        records = records == null ? List.of() : List.copyOf(records);
    }
}
