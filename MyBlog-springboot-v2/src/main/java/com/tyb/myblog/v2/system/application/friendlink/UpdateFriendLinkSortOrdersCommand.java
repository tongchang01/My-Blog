package com.tyb.myblog.v2.system.application.friendlink;

import java.util.List;

/**
 * 批量修改友链排序命令。
 */
public record UpdateFriendLinkSortOrdersCommand(
        List<FriendLinkSortItem> items
) {

    public UpdateFriendLinkSortOrdersCommand {
        items = items == null ? null : List.copyOf(items);
    }
}
