package com.tyb.myblog.v2.content.domain;

import java.time.LocalDateTime;
import java.util.List;

public record ArticleDetail(
        int id,
        String title,
        String summary,
        String content,
        String cover,
        CategorySummary category,
        AuthorSummary author,
        List<ArticleTagSummary> tags,
        boolean top,
        boolean featured,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public ArticleDetail {
        tags = tags == null ? List.of() : List.copyOf(tags);
    }
}
