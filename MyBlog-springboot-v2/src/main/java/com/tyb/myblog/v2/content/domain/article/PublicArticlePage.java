package com.tyb.myblog.v2.content.domain.article;

import java.util.List;

public record PublicArticlePage(
        List<PublicArticlePageItem> records,
        long total,
        int page,
        int size) {
}
