package com.tyb.myblog.v2.content.infrastructure.persistence.repository;

import com.tyb.myblog.v2.content.application.article.ArticleStatisticsGateway;
import com.tyb.myblog.v2.content.application.article.ArticleStatisticsPolicySnapshot;
import com.tyb.myblog.v2.content.domain.article.ArticleStatus;
import com.tyb.myblog.v2.content.infrastructure.persistence.mapper.ArticleMapper;
import com.tyb.myblog.v2.content.infrastructure.persistence.projection.ArticleStatisticsPolicyRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 文章访问统计 application 端口的 MyBatis 适配器。
 */
@Repository
@RequiredArgsConstructor
public class MyBatisArticleStatisticsGateway
        implements ArticleStatisticsGateway {

    private final ArticleMapper mapper;

    @Override
    public Optional<ArticleStatisticsPolicySnapshot> findPolicy(
            long articleId) {
        ArticleStatisticsPolicyRow row =
                mapper.selectStatisticsPolicy(articleId);
        if (row == null) {
            return Optional.empty();
        }
        return Optional.of(new ArticleStatisticsPolicySnapshot(
                row.getId(),
                ArticleStatus.fromDatabase(row.getStatus())));
    }
}
