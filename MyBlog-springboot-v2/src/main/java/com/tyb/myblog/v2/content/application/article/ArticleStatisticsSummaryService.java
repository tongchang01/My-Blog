package com.tyb.myblog.v2.content.application.article;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 为访问统计一次批量补齐文章标题。 */
@Service
@RequiredArgsConstructor
public class ArticleStatisticsSummaryService {

    private final ArticleStatisticsGateway gateway;

    public Map<Long, String> findTitles(Set<Long> articleIds) {
        if (articleIds.isEmpty()) {
            return Map.of();
        }
        return gateway.findSummaries(articleIds).stream()
                .collect(Collectors.toUnmodifiableMap(
                        ArticleStatisticsSummary::articleId,
                        ArticleStatisticsSummary::title));
    }
}
