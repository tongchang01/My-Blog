package com.tyb.myblog.v2.content.domain.article;

import java.time.LocalDateTime;

/** PASSWORD 文章访问令牌的持久化记录，明文永不落库。 */
public record ArticleAccessTokenRecord(
        long articleId,
        String tokenHash,
        LocalDateTime expiresAt) {
}
