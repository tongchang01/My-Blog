package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.content.domain.FeaturedArticles;

import java.util.List;

public record FeaturedArticlesResponse(
        ArticleSummaryResponse topArticle,
        List<ArticleSummaryResponse> featuredArticles
) {

    static FeaturedArticlesResponse from(FeaturedArticles featuredArticles) {
        return new FeaturedArticlesResponse(
                featuredArticles.topArticle().map(ArticleSummaryResponse::from).orElse(null),
                featuredArticles.featuredArticles().stream().map(ArticleSummaryResponse::from).toList());
    }
}
