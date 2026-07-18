package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.content.application.article.ArticleAccessTokenResult;

import java.time.LocalDateTime;

/** 解锁成功后返回一次的文章访问令牌。 */
public record PublicArticleUnlockVO(
        String token,
        LocalDateTime expiresAt) {

    static PublicArticleUnlockVO from(ArticleAccessTokenResult result) {
        return new PublicArticleUnlockVO(result.token(), result.expiresAt());
    }
}
