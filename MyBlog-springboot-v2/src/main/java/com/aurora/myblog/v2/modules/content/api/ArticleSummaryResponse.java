package com.aurora.myblog.v2.modules.content.api;

import com.aurora.myblog.v2.modules.content.domain.ArticleSummary;

import java.time.LocalDateTime;
import java.util.List;

public record ArticleSummaryResponse(
        int id,
        String title,
        String summary,
        String cover,
        ArticleCategoryResponse category,
        ArticleAuthorResponse author,
        List<ArticleTagResponse> tags,
        boolean top,
        boolean featured,
        LocalDateTime createdAt
) {

    static ArticleSummaryResponse from(ArticleSummary article) {
        return new ArticleSummaryResponse(
                article.id(),
                article.title(),
                article.summary(),
                article.cover(),
                ArticleCategoryResponse.from(article.category()),
                ArticleAuthorResponse.from(article.author()),
                article.tags().stream().map(ArticleTagResponse::from).toList(),
                article.top(),
                article.featured(),
                article.createdAt());
    }
}
