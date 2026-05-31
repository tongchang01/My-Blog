package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.content.domain.ArticleDetail;

import java.time.LocalDateTime;
import java.util.List;

public record ArticleDetailResponse(
        int id,
        String title,
        String summary,
        String content,
        String cover,
        ArticleCategoryResponse category,
        ArticleAuthorResponse author,
        List<ArticleTagResponse> tags,
        boolean top,
        boolean featured,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    static ArticleDetailResponse from(ArticleDetail article) {
        return new ArticleDetailResponse(
                article.id(),
                article.title(),
                article.summary(),
                article.content(),
                article.cover(),
                ArticleCategoryResponse.from(article.category()),
                ArticleAuthorResponse.from(article.author()),
                article.tags().stream().map(ArticleTagResponse::from).toList(),
                article.top(),
                article.featured(),
                article.createdAt(),
                article.updatedAt());
    }
}
