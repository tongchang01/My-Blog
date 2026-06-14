package com.tyb.myblog.v2.system.application.friendlink;

import java.util.List;

/**
 * 后台友链分页结果。
 */
public record FriendLinkPageResult(
        List<FriendLinkResult> records,
        long total,
        int page,
        int size
) {

    public FriendLinkPageResult {
        records = records == null ? List.of() : List.copyOf(records);
    }
}
