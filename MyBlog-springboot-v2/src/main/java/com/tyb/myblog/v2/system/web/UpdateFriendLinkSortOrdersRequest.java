package com.tyb.myblog.v2.system.web;

import com.tyb.myblog.v2.system.application.friendlink.FriendLinkSortItem;
import com.tyb.myblog.v2.system.application.friendlink.UpdateFriendLinkSortOrdersCommand;

import java.util.List;

/**
 * 友链批量排序请求。
 */
public record UpdateFriendLinkSortOrdersRequest(
        List<Item> items
) {

    public UpdateFriendLinkSortOrdersCommand toCommand() {
        return new UpdateFriendLinkSortOrdersCommand(
                items == null
                        ? null
                        : items.stream()
                                .map(item -> item == null
                                        ? null
                                        : new FriendLinkSortItem(
                                                item.id(),
                                                item.sortOrder()))
                                .toList());
    }

    public record Item(long id, int sortOrder) {
    }
}
