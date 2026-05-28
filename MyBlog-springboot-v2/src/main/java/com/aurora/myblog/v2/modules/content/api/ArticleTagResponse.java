package com.aurora.myblog.v2.modules.content.api;

import com.aurora.myblog.v2.modules.content.domain.ArticleTagSummary;

public record ArticleTagResponse(int id, String name) {

    static ArticleTagResponse from(ArticleTagSummary tag) {
        return new ArticleTagResponse(tag.id(), tag.name());
    }
}
