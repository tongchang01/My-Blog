package com.tyb.myblog.v2.content.domain;

import java.util.List;
import java.util.Optional;

public record FeaturedArticles(Optional<ArticleSummary> topArticle, List<ArticleSummary> featuredArticles) {

    public FeaturedArticles {
        topArticle = topArticle == null ? Optional.empty() : topArticle;
        featuredArticles = featuredArticles == null ? List.of() : List.copyOf(featuredArticles);
    }
}
