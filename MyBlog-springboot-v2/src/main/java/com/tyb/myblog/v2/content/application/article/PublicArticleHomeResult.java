package com.tyb.myblog.v2.content.application.article;

public record PublicArticleHomeResult(
        PublicArticlePageResult.Item pinnedArticle,
        java.util.List<PublicArticlePageResult.Item> featuredArticles,
        PublicArticlePageResult articles) {
}
