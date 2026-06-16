package com.tyb.myblog.v2.content.domain.article;

import java.time.LocalDateTime;

public record DeletedArticlePageItem(
        long id,
        String titleZh,
        String titleJa,
        String titleEn,
        ArticleStatus status,
        Long categoryId,
        LocalDateTime deletedAt,
        Long deletedBy) {
}
