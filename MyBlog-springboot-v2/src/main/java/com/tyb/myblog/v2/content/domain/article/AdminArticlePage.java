package com.tyb.myblog.v2.content.domain.article;

import java.util.List;

/**
 * 后台文章领域分页结果。
 */
public record AdminArticlePage(
        List<AdminArticlePageItem> records,
        long total,
        int page,
        int size) {

    public AdminArticlePage {
        records = records == null ? List.of() : List.copyOf(records);
    }
}
