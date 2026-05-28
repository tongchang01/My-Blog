package com.aurora.myblog.v2.modules.content.domain;

import com.aurora.myblog.v2.common.web.PageResponse;

public interface ArticleReader {

    PageResponse<ArticleSummary> listPublishedArticles(ArticlePageQuery query);

    PageResponse<ArticleSummary> listPublishedArticlesByCategory(int categoryId, ArticlePageQuery query);

    PageResponse<ArticleSummary> listPublishedArticlesByTag(int tagId, ArticlePageQuery query);
}
