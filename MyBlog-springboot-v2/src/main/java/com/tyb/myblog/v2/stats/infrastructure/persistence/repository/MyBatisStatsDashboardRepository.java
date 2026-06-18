package com.tyb.myblog.v2.stats.infrastructure.persistence.repository;

import com.tyb.myblog.v2.stats.domain.DailyTrafficPoint;
import com.tyb.myblog.v2.stats.domain.LanguageTraffic;
import com.tyb.myblog.v2.stats.domain.StatsDashboardRepository;
import com.tyb.myblog.v2.stats.domain.StatsLanguage;
import com.tyb.myblog.v2.stats.domain.TopArticleTraffic;
import com.tyb.myblog.v2.stats.infrastructure.persistence.mapper.PageViewAggregationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/** 后台统计总览查询端口的 MyBatis 适配器。 */
@Repository
@RequiredArgsConstructor
public class MyBatisStatsDashboardRepository
        implements StatsDashboardRepository {

    private final PageViewAggregationMapper mapper;

    @Override
    public List<DailyTrafficPoint> findTrend(
            LocalDate from, LocalDate to) {
        return mapper.selectTrend(from, to).stream()
                .map(row -> new DailyTrafficPoint(
                        row.getStatDate(), row.getPv(), row.getUv()))
                .toList();
    }

    @Override
    public List<TopArticleTraffic> findTopArticles(
            LocalDate from, LocalDate to, int limit) {
        return mapper.selectTopArticles(from, to, limit).stream()
                .map(row -> new TopArticleTraffic(
                        row.getArticleId(),
                        row.getPv(),
                        row.getDailyUvSum()))
                .toList();
    }

    @Override
    public List<LanguageTraffic> findLanguages(
            LocalDate from, LocalDate to) {
        return mapper.selectLanguages(from, to).stream()
                .map(row -> new LanguageTraffic(
                        StatsLanguage.fromCode(row.getLang()),
                        row.getPv()))
                .toList();
    }
}
