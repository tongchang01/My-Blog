package com.tyb.myblog.v2.content.application.article;

import java.util.List;

public record PublicArticleHomeResult(
        PublicArticlePageResult.Item pinnedArticle,
        List<PublicArticlePageResult.Item> featuredArticles,
        List<PublicArticlePageResult.Item> articles) {
}
