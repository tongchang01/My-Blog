package com.tyb.myblog.v2.stats.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 页面访问日聚合与短期明细清理端口。
 */
public interface PageViewAggregationRepository {

    List<DailyPageView> summarize(
            LocalDateTime start,
            LocalDateTime end,
            LocalDate statDate);

    void deleteDay(LocalDate statDate);

    void insertAll(List<DailyPageView> rows);

    int deleteRawBefore(LocalDateTime cutoff);
}
