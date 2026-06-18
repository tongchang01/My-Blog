package com.tyb.myblog.v2.stats.application;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.content.application.article.ArticleStatisticsSummaryService;
import com.tyb.myblog.v2.stats.domain.DailyTrafficPoint;
import com.tyb.myblog.v2.stats.domain.LanguageTraffic;
import com.tyb.myblog.v2.stats.domain.StatsDashboardRepository;
import com.tyb.myblog.v2.stats.domain.StatsLanguage;
import com.tyb.myblog.v2.stats.domain.TopArticleTraffic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StatsDashboardServiceTest {

    private final StatsDashboardRepository repository =
            mock(StatsDashboardRepository.class);
    private final ArticleStatisticsSummaryService summaryService =
            mock(ArticleStatisticsSummaryService.class);
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-06-18T03:00:00Z"),
            ZoneId.of("Asia/Tokyo"));

    private StatsDashboardService service;

    @BeforeEach
    void setUp() {
        service = new StatsDashboardService(
                repository,
                summaryService,
                new StatsAuthorization(),
                clock);
    }

    @Test
    void defaultsToThirtyDaysAndBuildsAccurateSummary() {
        LocalDate from = LocalDate.of(2026, 5, 20);
        LocalDate today = LocalDate.of(2026, 6, 18);
        when(repository.findTrend(from, today))
                .thenReturn(List.of(new DailyTrafficPoint(
                        today,
                        48,
                        31)));
        when(repository.findTrend(today, today))
                .thenReturn(List.of(new DailyTrafficPoint(
                        today,
                        48,
                        31)));
        when(repository.findTopArticles(from, today, 10))
                .thenReturn(List.of(new TopArticleTraffic(
                        100L,
                        320,
                        180)));
        when(repository.findLanguages(from, today))
                .thenReturn(List.of(new LanguageTraffic(
                        StatsLanguage.ZH,
                        48)));
        when(summaryService.findTitles(Set.of(100L)))
                .thenReturn(Map.of(100L, "文章标题"));

        StatsDashboardResult result = service.dashboard(
                principal("DEMO"),
                new StatsDashboardQuery(null, null));

        assertThat(result.periodPv()).isEqualTo(48);
        assertThat(result.todayPv()).isEqualTo(48);
        assertThat(result.todayUv()).isEqualTo(31);
        assertThat(result.averageDailyUv())
                .isEqualByComparingTo("1.0");
        assertThat(result.trend()).hasSize(30);
        assertThat(result.trend().get(0).date()).isEqualTo(from);
        assertThat(result.topArticles().get(0).title())
                .isEqualTo("文章标题");
        assertThat(result.topArticles().get(0).dailyUvSum())
                .isEqualTo(180);
        assertThat(result.languageDistribution().get(0).ratio())
                .isEqualByComparingTo("1.0000");
    }

    @Test
    void validatesRangeAndReadableRole() {
        assertValidation(new StatsDashboardQuery(
                LocalDate.of(2026, 6, 1), null));
        assertValidation(new StatsDashboardQuery(
                LocalDate.of(2026, 6, 2),
                LocalDate.of(2026, 6, 1)));
        assertValidation(new StatsDashboardQuery(
                LocalDate.of(2025, 6, 17),
                LocalDate.of(2026, 6, 18)));

        assertThatThrownBy(() -> service.dashboard(
                principal("GUEST"),
                new StatsDashboardQuery(null, null)))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).code())
                .isEqualTo(ApiErrorCode.FORBIDDEN);
    }

    @Test
    void keepsCustomRangeContinuousAndReadsTodaySeparately() {
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 6, 2);
        LocalDate today = LocalDate.of(2026, 6, 18);
        when(repository.findTrend(from, to)).thenReturn(List.of(
                new DailyTrafficPoint(to, 0, 0)));
        when(repository.findTrend(today, today)).thenReturn(List.of(
                new DailyTrafficPoint(today, 9, 7)));
        when(repository.findTopArticles(from, to, 10)).thenReturn(List.of(
                new TopArticleTraffic(999L, 0, 0)));
        when(repository.findLanguages(from, to)).thenReturn(List.of(
                new LanguageTraffic(StatsLanguage.ZH, 0)));
        when(summaryService.findTitles(Set.of(999L)))
                .thenReturn(Map.of());

        StatsDashboardResult result = service.dashboard(
                principal("ADMIN"),
                new StatsDashboardQuery(from, to));

        assertThat(result.trend()).hasSize(2);
        assertThat(result.trend().get(0))
                .isEqualTo(new StatsDashboardResult.TrendPoint(
                        from, 0, 0));
        assertThat(result.todayPv()).isEqualTo(9);
        assertThat(result.todayUv()).isEqualTo(7);
        assertThat(result.topArticles().get(0).title()).isNull();
        assertThat(result.languageDistribution().get(0).ratio())
                .isEqualByComparingTo("0.0000");
        verify(repository).findTrend(today, today);
    }

    @Test
    void acceptsInclusiveRangeOfExactlyThreeHundredSixtySixDays() {
        assertThatCode(() -> service.dashboard(
                principal("ADMIN"),
                new StatsDashboardQuery(
                        LocalDate.of(2025, 6, 18),
                        LocalDate.of(2026, 6, 18))))
                .doesNotThrowAnyException();
    }

    private void assertValidation(StatsDashboardQuery query) {
        assertThatThrownBy(() -> service.dashboard(
                principal("ADMIN"),
                query))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).code())
                .isEqualTo(ApiErrorCode.VALIDATION_ERROR);
    }

    private AuthenticatedPrincipal principal(String role) {
        return new AuthenticatedPrincipal(
                "1001",
                role.toLowerCase(),
                List.of(role));
    }
}
