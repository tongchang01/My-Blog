package com.tyb.myblog.v2.content.application.article;

import java.time.LocalDateTime;
import java.util.List;

public record PublicArticlePageResult(
        List<Item> records,
        long total,
        int page,
        int size) {

    public record Item(
            long id,
            String title,
            String summary,
            Long categoryId,
            String categoryName,
            String slug,
            LocalDateTime publishAt,
            String coverUrl,
            int commentCount,
            List<PublicArticleTagResult> tags,
            LocalDateTime createdAt,
            boolean locked) {
    }
}
