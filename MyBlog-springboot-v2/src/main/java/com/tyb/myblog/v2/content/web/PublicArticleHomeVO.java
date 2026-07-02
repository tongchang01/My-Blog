package com.tyb.myblog.v2.content.web;

import java.util.List;

public record PublicArticleHomeVO(
        PublicArticlePageItemVO pinnedArticle,
        List<PublicArticlePageItemVO> featuredArticles,
        List<PublicArticlePageItemVO> articles) {
}
