package com.tyb.myblog.v2.stats.domain;

import java.time.LocalDate;
import java.util.List;

/** 后台访问统计总览查询端口。 */
public interface StatsDashboardRepository {

    List<DailyTrafficPoint> findTrend(LocalDate from, LocalDate to);

    List<TopArticleTraffic> findTopArticles(
            LocalDate from, LocalDate to, int limit);

    List<LanguageTraffic> findLanguages(LocalDate from, LocalDate to);
}
