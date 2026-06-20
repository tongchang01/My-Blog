package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.content.application.article.PublicArticleTagResult;

public record PublicArticleTagVO(
        String id,
        String name,
        String slug) {

    static PublicArticleTagVO from(PublicArticleTagResult result) {
        return new PublicArticleTagVO(
                Long.toString(result.id()),
                result.name(),
                result.slug());
    }
}
