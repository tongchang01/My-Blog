package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.content.domain.ArticleAccessToken;

import java.time.Instant;

/**
 * 受保护文章访问令牌响应。
 *
 * @param articleId    文章 ID
 * @param accessToken  临时访问令牌
 * @param expiresAt    令牌过期时间
 */
public record ArticleAccessResponse(int articleId, String accessToken, Instant expiresAt) {

    /**
     * 从领域访问令牌转换为接口响应。
     */
    static ArticleAccessResponse from(int articleId, ArticleAccessToken token) {
        return new ArticleAccessResponse(articleId, token.value(), token.expiresAt());
    }
}
