package com.tyb.myblog.v2.stats.application;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.content.application.article.ArticleStatisticsSummaryService;
import com.tyb.myblog.v2.stats.domain.DailyTrafficPoint;
import com.tyb.myblog.v2.stats.domain.LanguageTraffic;
import com.tyb.myblog.v2.stats.domain.StatsDashboardRepository;
import com.tyb.myblog.v2.stats.domain.TopArticleTraffic;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 后台访问统计总览应用服务。 */
@Service
@RequiredArgsConstructor
public class StatsDashboardService {

    private static final int DEFAULT_DAYS = 30;
    private static final int MAX_DAYS = 366;
    private static final int TOP_LIMIT = 10;

    private final StatsDashboardRepository repository;
    private final ArticleStatisticsSummaryService summaryService;
    private final StatsAuthorization authorization;
    private final Clock clock;

    public StatsDashboardResult dashboard(
            AuthenticatedPrincipal principal,
            StatsDashboardQuery query) {
        authorization.requireReadable(principal);
        LocalDate today = LocalDate.now(clock);
        DateRange range = resolveRange(query, today);

        List<StatsDashboardResult.TrendPoint> trend = completeTrend(
                range,
                repository.findTrend(range.from(), range.to()));
        long periodPv = trend.stream()
                .mapToLong(StatsDashboardResult.TrendPoint::pv)
                .sum();
        long periodDailyUv = trend.stream()
                .mapToLong(StatsDashboardResult.TrendPoint::uv)
                .sum();

        DailyTrafficPoint todayTraffic = repository.findTrend(today, today)
                .stream()
                .findFirst()
                .orElse(new DailyTrafficPoint(today, 0, 0));
        List<TopArticleTraffic> topTraffic = repository.findTopArticles(
                range.from(), range.to(), TOP_LIMIT);
        Set<Long> articleIds = topTraffic.stream()
                .map(TopArticleTraffic::articleId)
                .collect(Collectors.toSet());
        Map<Long, String> titles = summaryService.findTitles(articleIds);

        return new StatsDashboardResult(
                periodPv,
                todayTraffic.pv(),
                todayTraffic.uv(),
                BigDecimal.valueOf(periodDailyUv)
                        .divide(BigDecimal.valueOf(range.days()),
                                1,
                                RoundingMode.HALF_UP),
                trend,
                topTraffic.stream()
                        .map(metric -> new StatsDashboardResult.TopArticle(
                                metric.articleId(),
                                titles.get(metric.articleId()),
                                metric.pv(),
                                metric.dailyUvSum()))
                        .toList(),
                toLanguageDistribution(
                        repository.findLanguages(range.from(), range.to()),
                        periodPv));
    }

    private DateRange resolveRange(
            StatsDashboardQuery query,
            LocalDate today) {
        LocalDate from = query == null ? null : query.from();
        LocalDate to = query == null ? null : query.to();
        if (from == null && to == null) {
            return new DateRange(today.minusDays(DEFAULT_DAYS - 1L), today);
        }
        if (from == null || to == null || from.isAfter(to)) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR);
        }
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        if (days > MAX_DAYS) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR);
        }
        return new DateRange(from, to);
    }

    private List<StatsDashboardResult.TrendPoint> completeTrend(
            DateRange range,
            List<DailyTrafficPoint> stored) {
        Map<LocalDate, DailyTrafficPoint> byDate = new LinkedHashMap<>();
        stored.forEach(point -> byDate.put(point.date(), point));
        return range.from().datesUntil(range.to().plusDays(1))
                .map(date -> {
                    DailyTrafficPoint point = byDate.get(date);
                    return point == null
                            ? new StatsDashboardResult.TrendPoint(date, 0, 0)
                            : new StatsDashboardResult.TrendPoint(
                                    date, point.pv(), point.uv());
                })
                .toList();
    }

    private List<StatsDashboardResult.LanguageDistribution>
            toLanguageDistribution(
                    List<LanguageTraffic> languages,
                    long periodPv) {
        return languages.stream()
                .map(metric -> new StatsDashboardResult.LanguageDistribution(
                        metric.language().code(),
                        metric.pv(),
                        periodPv == 0
                                ? BigDecimal.ZERO.setScale(4)
                                : BigDecimal.valueOf(metric.pv())
                                        .divide(BigDecimal.valueOf(periodPv),
                                                4,
                                                RoundingMode.HALF_UP)))
                .toList();
    }

    private record DateRange(LocalDate from, LocalDate to) {
        long days() {
            return ChronoUnit.DAYS.between(from, to) + 1;
        }
    }
}
