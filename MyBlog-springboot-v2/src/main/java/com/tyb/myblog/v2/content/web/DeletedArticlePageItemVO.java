package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.content.domain.article.ArticleStatus;

import java.time.LocalDateTime;

public record DeletedArticlePageItemVO(
        long id,
        String titleZh,
        String titleJa,
        String titleEn,
        ArticleStatus status,
        Long categoryId,
        LocalDateTime deletedAt,
        Long deletedBy) {
}
