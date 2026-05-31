package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.content.domain.ArticleTagSummary;

public record ArticleTagResponse(int id, String name) {

    static ArticleTagResponse from(ArticleTagSummary tag) {
        return new ArticleTagResponse(tag.id(), tag.name());
    }
}
