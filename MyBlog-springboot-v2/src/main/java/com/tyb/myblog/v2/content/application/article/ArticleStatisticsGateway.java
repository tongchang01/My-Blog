package com.tyb.myblog.v2.content.application.article;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * content 向访问统计暴露的最小文章查询端口。
 */
public interface ArticleStatisticsGateway {

    Optional<ArticleStatisticsPolicySnapshot> findPolicy(long articleId);

    List<ArticleStatisticsSummary> findSummaries(Set<Long> articleIds);
}
