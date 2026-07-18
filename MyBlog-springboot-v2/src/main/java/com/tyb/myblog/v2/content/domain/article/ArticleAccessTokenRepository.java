package com.tyb.myblog.v2.content.domain.article;

import java.time.LocalDateTime;

/** PASSWORD 文章短期访问令牌端口。 */
public interface ArticleAccessTokenRepository {

    void save(ArticleAccessTokenRecord token);

    boolean existsActive(long articleId, String tokenHash, LocalDateTime now);

    int revokeAllByArticleId(long articleId);
}
