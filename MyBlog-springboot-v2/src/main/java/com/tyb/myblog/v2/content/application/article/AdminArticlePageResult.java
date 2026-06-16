package com.tyb.myblog.v2.content.application.article;

import com.tyb.myblog.v2.content.domain.article.AdminArticlePageItem;

import java.util.List;

/**
 * 后台文章分页应用结果。
 */
public record AdminArticlePageResult(
        List<AdminArticlePageItem> records,
        long total,
        int page,
        int size) {

    public AdminArticlePageResult {
        records = records == null ? List.of() : List.copyOf(records);
    }
}
