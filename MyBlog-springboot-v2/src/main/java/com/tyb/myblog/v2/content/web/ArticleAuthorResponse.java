package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.content.domain.AuthorSummary;

public record ArticleAuthorResponse(int id, String nickname, String avatar) {

    static ArticleAuthorResponse from(AuthorSummary author) {
        return new ArticleAuthorResponse(author.id(), author.nickname(), author.avatar());
    }
}
