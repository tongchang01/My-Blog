package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.content.application.category.CategorySortItem;
import com.tyb.myblog.v2.content.application.category.UpdateCategorySortOrdersCommand;

import java.util.List;

/**
 * 分类批量排序 HTTP 请求。
 */
public record UpdateCategorySortOrdersRequest(
        List<Item> items) {

    public UpdateCategorySortOrdersCommand toCommand() {
        return new UpdateCategorySortOrdersCommand(
                items == null
                        ? null
                        : items.stream()
                                .map(item -> item == null
                                        ? null
                                        : new CategorySortItem(
                                                item.id(),
                                                item.sortOrder()))
                                .toList());
    }

    public record Item(long id, int sortOrder) {
    }
}
