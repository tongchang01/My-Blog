package com.aurora.myblog.v2.modules.content.api;

import com.aurora.myblog.v2.modules.content.domain.AuthorSummary;

public record ArticleAuthorResponse(int id, String nickname, String avatar) {

    static ArticleAuthorResponse from(AuthorSummary author) {
        return new ArticleAuthorResponse(author.id(), author.nickname(), author.avatar());
    }
}
