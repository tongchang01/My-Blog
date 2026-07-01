package com.tyb.myblog.v2.content.domain.article;

import java.util.List;

public record PublicArticleHome(
        PublicArticlePageItem pinnedArticle,
        List<PublicArticlePageItem> featuredArticles,
        List<PublicArticlePageItem> articles) {
}
