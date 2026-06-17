package com.tyb.myblog.v2.content.infrastructure.persistence.repository;

import com.tyb.myblog.v2.content.application.article.ArticleCommentGateway;
import com.tyb.myblog.v2.content.application.article.ArticleCommentPolicySnapshot;
import com.tyb.myblog.v2.content.domain.article.ArticleStatus;
import com.tyb.myblog.v2.content.infrastructure.persistence.mapper.ArticleMapper;
import com.tyb.myblog.v2.content.infrastructure.persistence.projection.ArticleCommentPolicyRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MyBatisArticleCommentGateway implements ArticleCommentGateway {

    private final ArticleMapper mapper;

    @Override
    public Optional<ArticleCommentPolicySnapshot> findCommentPolicy(
            long articleId) {
        ArticleCommentPolicyRow row = mapper.selectCommentPolicy(articleId);
        if (row == null) {
            return Optional.empty();
        }
        return Optional.of(new ArticleCommentPolicySnapshot(
                row.getId(),
                ArticleStatus.fromDatabase(row.getStatus()),
                row.getCommentCount() == null ? 0 : row.getCommentCount()));
    }

    @Override
    public boolean incrementCommentCount(long articleId, int delta) {
        return mapper.incrementCommentCount(articleId, delta) == 1;
    }
}
