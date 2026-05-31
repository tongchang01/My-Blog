package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.content.domain.ArticleAccessToken;

import java.time.Instant;

public record ArticleAccessResponse(int articleId, String accessToken, Instant expiresAt) {

    static ArticleAccessResponse from(int articleId, ArticleAccessToken token) {
        return new ArticleAccessResponse(articleId, token.value(), token.expiresAt());
    }
}
