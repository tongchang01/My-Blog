package com.tyb.myblog.v2.stats.application;

import com.tyb.myblog.v2.stats.domain.DailyPageView;
import com.tyb.myblog.v2.stats.domain.PageViewAggregationRepository;
import com.tyb.myblog.v2.stats.domain.StatsLanguage;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PageViewDailyRebuildServiceTest {

    @Test
    void replacesOneJstDayInOrder() {
        PageViewAggregationRepository repository =
                mock(PageViewAggregationRepository.class);
        Clock clock = Clock.system(ZoneId.of("Asia/Tokyo"));
        PageViewDailyRebuildService service =
                new PageViewDailyRebuildService(repository, clock);
        LocalDate date = LocalDate.of(2026, 6, 18);
        List<DailyPageView> rows = List.of(new DailyPageView(
                100L,
                StatsLanguage.ZH,
                date,
                3,
                2));
        when(repository.summarize(
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay(),
                date)).thenReturn(rows);

        service.rebuild(date);

        InOrder order = inOrder(repository);
        order.verify(repository).summarize(
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay(),
                date);
        order.verify(repository).deleteDay(date);
        order.verify(repository).insertAll(rows);
    }
}
