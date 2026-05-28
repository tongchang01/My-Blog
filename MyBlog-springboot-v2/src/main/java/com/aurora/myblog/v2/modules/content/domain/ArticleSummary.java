package com.aurora.myblog.v2.modules.content.domain;

import java.time.LocalDateTime;
import java.util.List;

public record ArticleSummary(
        int id,
        String title,
        String summary,
        String cover,
        CategorySummary category,
        AuthorSummary author,
        List<ArticleTagSummary> tags,
        boolean top,
        boolean featured,
        LocalDateTime createdAt
) {

    public ArticleSummary {
        tags = tags == null ? List.of() : List.copyOf(tags);
    }
}
