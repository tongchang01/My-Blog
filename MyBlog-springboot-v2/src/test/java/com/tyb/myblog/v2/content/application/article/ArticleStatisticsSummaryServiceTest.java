package com.tyb.myblog.v2.content.application.article;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArticleStatisticsSummaryServiceTest {

    private final ArticleStatisticsGateway gateway =
            mock(ArticleStatisticsGateway.class);
    private final ArticleStatisticsSummaryService service =
            new ArticleStatisticsSummaryService(gateway);

    @Test
    void resolvesTitlesInOneBatchAndKeepsMissingIdsAbsent() {
        when(gateway.findSummaries(Set.of(100L, 101L)))
                .thenReturn(List.of(
                        new ArticleStatisticsSummary(100L, "文章标题")));

        Map<Long, String> titles = service.findTitles(
                Set.of(100L, 101L));

        assertThat(titles).containsOnlyKeys(100L);
        assertThat(titles.get(100L)).isEqualTo("文章标题");
        verify(gateway).findSummaries(Set.of(100L, 101L));
    }

    @Test
    void skipsGatewayForEmptyIds() {
        assertThat(service.findTitles(Set.of())).isEmpty();
        verify(gateway, never()).findSummaries(Set.of());
    }
}
