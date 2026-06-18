package com.tyb.myblog.v2.stats.application;

import com.tyb.myblog.v2.stats.domain.PageViewAggregationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class PageViewMaintenanceServiceTest {

    private final PageViewDailyRebuildService rebuildService =
            mock(PageViewDailyRebuildService.class);
    private final PageViewAggregationRepository repository =
            mock(PageViewAggregationRepository.class);
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-06-18T03:00:00Z"),
            ZoneId.of("Asia/Tokyo"));

    private PageViewMaintenanceService service;

    @BeforeEach
    void setUp() {
        service = new PageViewMaintenanceService(
                rebuildService,
                repository,
                clock);
    }

    @Test
    void rebuildsTodayAndYesterday() {
        service.rebuildCurrentWindow();

        verify(rebuildService).rebuild(LocalDate.of(2026, 6, 17));
        verify(rebuildService).rebuild(LocalDate.of(2026, 6, 18));
    }

    @Test
    void reconcilesNinetyDaysBeforeDeletingOlderDetails() {
        service.reconcileAndCleanup();

        verify(rebuildService, times(90)).rebuild(any(LocalDate.class));
        verify(rebuildService).rebuild(LocalDate.of(2026, 3, 21));
        verify(rebuildService).rebuild(LocalDate.of(2026, 6, 18));
        verify(repository).deleteRawBefore(
                LocalDateTime.of(2026, 3, 21, 0, 0));
    }
}
