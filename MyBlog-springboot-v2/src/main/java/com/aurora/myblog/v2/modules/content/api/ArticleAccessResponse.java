package com.aurora.myblog.v2.modules.content.api;

import com.aurora.myblog.v2.modules.content.domain.ArticleAccessToken;

import java.time.Instant;

public record ArticleAccessResponse(int articleId, String accessToken, Instant expiresAt) {

    static ArticleAccessResponse from(int articleId, ArticleAccessToken token) {
        return new ArticleAccessResponse(articleId, token.value(), token.expiresAt());
    }
}
