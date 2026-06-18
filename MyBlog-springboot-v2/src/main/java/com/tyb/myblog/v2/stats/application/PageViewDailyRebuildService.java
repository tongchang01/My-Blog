package com.tyb.myblog.v2.stats.application;

import com.tyb.myblog.v2.stats.domain.DailyPageView;
import com.tyb.myblog.v2.stats.domain.PageViewAggregationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 单个 JST 日期的访问聚合重算事务。
 */
@Service
@RequiredArgsConstructor
public class PageViewDailyRebuildService {

    private final PageViewAggregationRepository repository;
    private final Clock clock;

    @Transactional
    public void rebuild(LocalDate date) {
        LocalDateTime start = date.atStartOfDay(clock.getZone())
                .toLocalDateTime();
        LocalDateTime end = date.plusDays(1)
                .atStartOfDay(clock.getZone())
                .toLocalDateTime();
        List<DailyPageView> rows =
                repository.summarize(start, end, date);
        repository.deleteDay(date);
        repository.insertAll(rows);
    }
}
