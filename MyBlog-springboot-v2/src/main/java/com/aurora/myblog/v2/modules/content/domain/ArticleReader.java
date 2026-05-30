package com.aurora.myblog.v2.modules.content.domain;

import com.aurora.myblog.v2.common.web.PageResponse;

import java.util.Optional;

public interface ArticleReader {

    PageResponse<ArticleSummary> listPublishedArticles(ArticlePageQuery query);

    PageResponse<ArticleSummary> listPublishedArticlesByCategory(int categoryId, ArticlePageQuery query);

    PageResponse<ArticleSummary> listPublishedArticlesByTag(int tagId, ArticlePageQuery query);

    FeaturedArticles findFeaturedArticles();

    PageResponse<ArchiveMonth> listPublishedArchives(ArticlePageQuery query);

    Optional<ArticleAccessCheck> findArticleAccessCheckById(int articleId);

    Optional<ArticleDetail> findPublishedArticleById(int articleId);

    Optional<ArticleDetail> findAccessibleArticleById(int articleId);
}
