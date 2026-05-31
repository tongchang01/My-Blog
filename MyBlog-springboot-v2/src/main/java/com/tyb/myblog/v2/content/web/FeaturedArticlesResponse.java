package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.content.domain.FeaturedArticles;

import java.util.List;

/**
 * 首页推荐文章响应。
 *
 * @param topArticle       置顶文章
 * @param featuredArticles 推荐文章列表
 */
public record FeaturedArticlesResponse(
        ArticleSummaryResponse topArticle,
        List<ArticleSummaryResponse> featuredArticles
) {

    /**
     * 从领域推荐文章组合转换为接口响应。
     */
    static FeaturedArticlesResponse from(FeaturedArticles featuredArticles) {
        return new FeaturedArticlesResponse(
                featuredArticles.topArticle().map(ArticleSummaryResponse::from).orElse(null),
                featuredArticles.featuredArticles().stream().map(ArticleSummaryResponse::from).toList());
    }
}
