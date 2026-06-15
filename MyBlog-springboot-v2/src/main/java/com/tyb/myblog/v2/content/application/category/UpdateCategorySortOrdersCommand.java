package com.tyb.myblog.v2.content.application.category;

import java.util.List;

/**
 * 批量更新分类排序命令。
 */
public record UpdateCategorySortOrdersCommand(
        List<CategorySortItem> items) {
}
