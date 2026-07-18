package com.tyb.myblog.v2.content.infrastructure.persistence.repository;

import com.tyb.myblog.v2.content.domain.article.ArticleAccessTokenRecord;
import com.tyb.myblog.v2.content.domain.article.ArticleAccessTokenRepository;
import com.tyb.myblog.v2.content.infrastructure.persistence.entity.ArticleAccessTokenEntity;
import com.tyb.myblog.v2.content.infrastructure.persistence.mapper.ArticleAccessTokenMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class MyBatisArticleAccessTokenRepository implements ArticleAccessTokenRepository {

    private final ArticleAccessTokenMapper mapper;

    @Override
    public void save(ArticleAccessTokenRecord token) {
        ArticleAccessTokenEntity entity = new ArticleAccessTokenEntity();
        entity.setArticleId(token.articleId());
        entity.setTokenHash(token.tokenHash());
        entity.setExpiresAt(token.expiresAt());
        entity.setRevoked(0);
        if (mapper.insert(entity) != 1) {
            throw new IllegalStateException("文章访问授权写入失败");
        }
    }

    @Override
    public boolean existsActive(long articleId, String tokenHash, LocalDateTime now) {
        return mapper.countActive(articleId, tokenHash, now) == 1;
    }

    @Override
    public int revokeAllByArticleId(long articleId) {
        return mapper.revokeActiveByArticleId(articleId);
    }
}
