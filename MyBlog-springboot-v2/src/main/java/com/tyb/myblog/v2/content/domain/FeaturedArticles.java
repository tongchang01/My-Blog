package com.tyb.myblog.v2.content.domain;

import java.util.List;
import java.util.Optional;

/**
 * 首页推荐文章组合。
 *
 * @param topArticle       置顶文章
 * @param featuredArticles 推荐文章列表
 */
public record FeaturedArticles(Optional<ArticleSummary> topArticle, List<ArticleSummary> featuredArticles) {

    /**
     * 规范空值并复制推荐文章列表。
     */
    public FeaturedArticles {
        topArticle = topArticle == null ? Optional.empty() : topArticle;
        featuredArticles = featuredArticles == null ? List.of() : List.copyOf(featuredArticles);
    }
}
