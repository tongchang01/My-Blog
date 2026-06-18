package com.tyb.myblog.v2.stats.application;

import com.tyb.myblog.v2.stats.domain.PageViewAggregationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.stream.IntStream;

/**
 * 近期访问聚合、历史补算与明细清理编排。
 */
@Service
@RequiredArgsConstructor
public class PageViewMaintenanceService {

    private static final int RETENTION_DAYS = 90;

    private final PageViewDailyRebuildService rebuildService;
    private final PageViewAggregationRepository repository;
    private final Clock clock;

    public void rebuildCurrentWindow() {
        LocalDate today = LocalDate.now(clock);
        rebuildService.rebuild(today.minusDays(1));
        rebuildService.rebuild(today);
    }

    public void reconcileAndCleanup() {
        LocalDate today = LocalDate.now(clock);
        IntStream.range(0, RETENTION_DAYS)
                .mapToObj(today::minusDays)
                .sorted()
                .forEach(rebuildService::rebuild);
        repository.deleteRawBefore(
                today.minusDays(RETENTION_DAYS - 1)
                        .atStartOfDay());
    }
}
